(ns kalai.pass.kalai.a-annotate-ast
  (:require [meander.strategy.epsilon :as s]
            [meander.epsilon :as m]
            [clojure.tools.analyzer.ast :as ast]
            [kalai.util :as u]
            [kalai.types :as types]
            [clojure.string :as str])
  (:import (clojure.lang IMeta)))

(defn resolve-in-ast-ns
  "Tools analyzer does not evaluate metadata in bindings or arglists.
   They are symbols which we can resolve to vars.
   We have to resolve these symbols in the namespace that they were defined."
  [sym ast]
  (and
    (symbol? sym)
    (binding [*ns* (-> ast :env :ns find-ns)]
      (resolve sym))))

(defn resolve-kalias
  "We replace type aliases with their definition.
   We matched an AST node with meta data {:t T} where T is a var
   with meta {:kalias K}."
  [sym ast]
  (when-let [z (resolve-in-ast-ns sym ast)]
    (when (var? z)
      (:kalias (meta z)))))

(defn resolve-tag
  "We didn't find a t, so we resolve tag and convert it to a Kalai type."
  [sym ast]
  (or (get types/primitive-symbol-types sym)
      (when-let [c (resolve-in-ast-ns sym ast)]
        (when (class? c)
          ;; Clojure macro expands expressions out that create bindings,
          ;; and some of those will have unexpected types that we can just ignore
          (or (get types/java-types c) c)))))

;; TODO: what about type aliases in type aliases
(defn resolve-t
  "Takes a value that might have metadata,
  and an AST, and resolves the type"
  [x ast]
  (let [{:keys [t tag]} (meta x)]
    (if t
      (if (symbol? t)
        (resolve-kalias t ast)
        t)
      (resolve-tag tag ast))))

(def ref-vars
  #{#'atom
    #'ref
    #'agent})

(defn clojure-type [tag]
  (str/starts-with? (.getCanonicalName tag) "clojure.lang"))

(defn ast-t
  "Return the type represented by an AST node. `root` is an AST node that
  contains namespace information for `ast`. Type info returned is using the
  normalized form consisting of keywords, as described in [Design.md]."
  ([ast]
   (ast-t ast ast))
  ([ast root]
   (m/rewrite ast
     ;; (atom x)
     {:op   :invoke
      :fn   {:var (m/pred ref-vars)}
      :args [?value . _ ...]}
     ~(ast-t ?value ast)

     ;; ^{:t :long, :tag Long} x
     {:op   :with-meta
      :meta {:form {:t ?t :tag ?tag}}
      :expr ?expr}
     ~(or ?t
          (get types/java-types ?tag)
          (ast-t ?expr ast))

     ;; If we see user-provided type metadata on the binding name itself,
     ;; then return a normalized version of the type info data.
     ;; This enables the initial-value-to-binding propagation of type metadata.
     {:op   :binding
      :form (m/app #(resolve-t % root) (m/pred some? ?t))}
     ?t

     ;; Start a recursive search for the type, starting with the initial value.
     ;; Supports a thorough inference of type, like for `z` in this situation:
     ;; (let [^{:t :some-type} x some-val
     ;;       y x
     ;;       z y] ...)
     {:op   :binding
      :init (m/pred some? ?init)}
     ~(ast-t ?init ast)

     ;; A typical case: an identifier used as an expression that was defined
     ;; previously in this lexical scope
     {:op   :local
      :form (m/pred some? ?form)
      :env  {:locals {?form ?binding}}}
     ~(ast-t ?binding ast)

     ;; Users who use any non-Kalai-primitive types or custom types (ex:
     ;; for interop purposes), which are represented as Java clases/types, will
     ;; often times instantiate the Java class that represents the custom type.
     ;; In such cases, when the `new` operator is invoked, we can resolve the
     ;; symbol of the custom type's Java class (ex: 'String) into the type that
     ;; we expect the :t key to take in the metadata map (ex: :string).
     {:op    :new
      :class {:class (m/app #(resolve-tag % root) (m/pred some? ?t))}}
     ?t

     ;; Last resort: 1
     {:op  :const
      :val (m/pred some? ?val)}
     ~(get types/java-types (type ?val))

     ;; Last resort: Clojure type inferred
     ;; There are two cases when o-tag is not nil
     ;; 1. When there is a user defined class or a non primitive class/object
     ;; 2. When there is an initialization and the initial value is a Clojure collection type, keyword, etc
     ;; Case 2 is a special case of 1, however we want Clojure collection types to have more specific type information
     ;; and we assume that we can get this from the metadata on the identifier symbol,
     ;; or the metadata on the initial object value itself,
     ;; which we also assume is given in the Kalai form.
     ;; Therefore in case 2, we should ignore the less informative o-tag value.
     {:o-tag (m/pred some? ?o-tag)}
     ~(or (get types/java-types ?o-tag)
          (when (and ?o-tag (not (clojure-type ?o-tag)))
            ?o-tag))

     ?else
     nil)))

(defn t-from-meta [x]
  (:t (meta x)))

(defn propagate-ast-type
  "If possible, associate the representative type of `symbol-bind-site` or `from-ast` to `symbol-call-site`,
  otherwise, just return `symbol-call-site` exactly as-is.
  Return `symbol-call-site` as-is if:
    * `symbol-call-site` cannot take metadata
    * `symbol-call-site` already has type info, as inferred by truthy value for `:t` in
    metadata"
  [from-ast symbol-bind-site symbol-call-site ast]
  (if (and (instance? IMeta symbol-call-site)
           (not (t-from-meta symbol-call-site)))
    (u/maybe-meta-assoc symbol-call-site
                        :t (or (:t (meta symbol-bind-site))
                               (ast-t from-ast)
                               (resolve-tag (:tag (meta symbol-call-site)) ast)
                               (resolve-tag (:tag (meta symbol-bind-site)) ast))
                        :mut (:mut (meta symbol-bind-site)))
    symbol-call-site))

(defn set-coll-t [val t]
  (m/rewrite t
    {(m/pred #{:mmap :map}) [?kt ?vt]}
    ;;->
    ~(into (u/maybe-meta-assoc (empty val)
                               :t (or (:t (meta val)) t))
           (for [[k v] val]
             [(set-coll-t k ?kt) (set-coll-t v ?vt)]))

    {(m/pred #{:mvector :vector :mset :set}) [?it]}
    ;;->
    ~(into (u/maybe-meta-assoc (empty val)
                               :t (or (:t (meta val)) t))
           (for [x val]
             (set-coll-t x ?it)))

    ?else
    ~val))

;; form is used for emit on map literals and constants,
;; except if they are with-meta expressions
;; eg: ^{:t {:map [:long :long]} {1 1}   <- with-meta
;; vs {1 1}     <- const
;; vs {1 (inc 1)}   <- map
;; EXCEPT when working with files!!???!!!
(defn set-ast-t
  "We match against both the collection and the type because maps must have
  a valid map type and we need the key value sub-types."
  [ast t]
  (m/rewrite [ast t]
    ;; collections [] #{} {} ()
    [{:op   :const
      :form (m/pred coll? ?form)
      &     ?more}
     (m/and {?k ?v} ?t)]
    ;;->
    {:op   :const
     :form ~(if ?t
              (set-coll-t ?form ?t)
              ?form)
     &     ?more}

    [{:op   :with-meta
      :expr ?expr
      :form ?form
      &     ?more}
     ?t]
    ;;->
    {:op   :with-meta
     ;; TODO: only if we don't have a t
     :expr ~(do
              (set-ast-t ?expr ?t))
     :form ~(if ?t
              (set-coll-t ?form ?t)
              ?form)
     &     ?more}

    [{:op   :map
      :form ?form
      :keys [!keys ...]
      :vals [!vals ...]
      &     ?more}
     (m/and
       {(m/pred #{:mmap :map}) [?kt ?vt]}
       ?t)]
    ;;->
    {:op   :map
     :form ~(set-coll-t ?form ?t)
     :keys [(m/app set-ast-t !keys ?kt) ...]
     :vals [(m/app set-ast-t !vals ?vt) ...]
     &     ?more}

    [{:op    (m/pred #{:set :vector} ?op)
      :form  ?form
      :items [!items ...]
      &      ?more}
     (m/and
       {(m/pred #{:mvector :vector :mset :set}) [?it]}
       ?t)]
    ;;->
    {:op    ?op
     :form  ~(set-coll-t ?form ?t)
     :items [(m/app set-ast-t !items ?it) ...]
     &      ?more}

    ;; (atom []) put the type on the vector
    [{:op   :invoke
      :fn   {:op  :var
             ;; TODO: some duplicaton here with ref-vars in kalai-constructs
             :var (m/pred #{#'atom
                            #'ref
                            #'agent})
             :as  ?fn}
      :args [?x]
      &     ?more}
     ?t]
    ;;->
    {:op   :invoke
     :fn   ?fn
     :args [~(set-ast-t ?x ?t)]
     &     ?more}

    ;; else return ast unchanged
    [?ast ?t]
    ~?ast))

(defn normalize-t-in-ast
  "Normalizing t consists of:
  1. If t is a valid Kalai type, it must be used.
  2. If t is a var, look up the kalias, which must be a Kalai type.
  3. If there is a tag, convert it to a Kalai type, and use that as t.
  4. If there is initialization, use the normalized initialization t.
  5. For initializations, if no t is present, use the binding t if present."
  [ast]
  (m/rewrite ast
    ;; (def x) and (def x 1)
    (m/and
      {:op   :def
       :name ?name
       :init ?init
       &     ?more
       :as   ?ast}
      (m/let [?t (resolve-t ?name ?ast)
              ?init-t (ast-t ?init)])
      (m/guard (or ?t ?init-t)))
    ;;->
    {:op   :def
     :name ~(u/maybe-meta-assoc ?name :t (or ?t ?init-t))
     :init ~(when ?init
              (set-ast-t ?init (or ?init-t ?t)))
     &     ?more}

    ;; [x 1]
    (m/and
      {:op   :binding
       :form ?form
       :init ?init
       &     ?more
       :as   ?ast}
      (m/let [?t (resolve-t ?form ?ast)
              ?init-t (ast-t ?init)])
      (m/guard (or ?t ?init-t)))
    ;;->
    {:op   :binding
     :form ~(u/maybe-meta-assoc ?form :t (or ?t ?init-t))
     :init ~(set-ast-t ?init (or ?init-t ?t))
     &     ?more}

    ;; ([x y z])
    {:op   :fn-method
     :form (?params & ?body)
     &     ?more
     :as   ?ast}
    ;;->
    {:op   :fn-method
     :form (~(u/maybe-meta-assoc ?params :t (resolve-t ?params ?ast))
             & ?body)
     &     ?more}

    ;; otherwise leave the ast as is
    ?else
    ?else))

(def propagate-types-from-bindings-to-locals
  "We propagate type information which is stored in metadata
  from the the place where they are declared on a symbol
  to all future usages of that symbol in scope."
  ;; TODO: function call type inference would be nice
  (s/rewrite
    ;; TODO: this must happen after value->binding
    ;; locals are usages of a declared binding
    {:op   :local
     :form ?symbol-call-site
     :env  {:locals {?symbol-call-site {:form ?symbol-bind-site
                                    :init ?init}}
            :as     ?env}
     &     ?more
     :as   ?ast}
    ;;->
    {:op   :local
     :form ~(propagate-ast-type ?init ?symbol-bind-site ?symbol-call-site ?ast)
     :env  ?env
     &     ?more}

    ;; TODO: what about globals?

    ;; otherwise leave the ast as is
    ?else
    ?else))

(def erase-type-aliases
  "Takes a vector of ASTs,
  matches and removes kalias defs, leaves other ASTs alone."
  (s/rewrite
    [(m/or {:op   :def
            :meta {:form {:kalias (m/pred some? !kalias)}}
            :name !name}
           !ast) ...]
    ;;->
    (!ast ...)

    ?else
    ~(throw (ex-info "ASTs" {:else ?else}))))

(def annotate-vars
  (s/rewrite
    ;; annotate vars with their var as metadata so they can be identified later in the pipeline
    {:op   :var
     :var  ?var
     :form ?form
     &     ?ast}
    ;;->
    {:op   :var
     :var  ?var
     :form ~(u/maybe-meta-assoc ?form :var ?var)
     &     ?ast}

    ;; otherwise leave the ast as is
    ?else
    ?else))

(def annotate-news
  (s/rewrite
    ;; annotate vars with their var as metadata so they can be identified later in the pipeline
    {:op   :new
     :class {:form ?form
             :val ?type
             & ?more}
     &     ?ast}
    ;;->
    {:op   :new
     :class {:form ~(u/maybe-meta-assoc ?form :t ?type)
             :val ?type
             & ?more}
     &     ?ast}

    ;; otherwise leave the ast as is
    ?else
    ?else))

;; TODO: split this mini-pipeline into 3 passes under the ast folder
(defn rewrite
  "There is contextual information in the AST that is not available in s-expressions.
  The purpose of this pass is to capture that information and modify the s-expressions to contain what we need."
  [asts]
  (->> asts
       (erase-type-aliases)
       (map #(ast/prewalk % normalize-t-in-ast))
       (map #(ast/prewalk % propagate-types-from-bindings-to-locals))
       ;; TODO: this is here for a circular depedency,
       ;; between normalization and propagation,
       ;; but it doesn't solve [x 1, y x, z y] ...
       (map #(ast/prewalk % normalize-t-in-ast))
       (map #(ast/prewalk % annotate-vars))
       (map #(ast/prewalk % annotate-news))

       ;; TODO:
       ;; assert our invariant that everything has a type
       ;; separate pass on s-expressions

       ))
