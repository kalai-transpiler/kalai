(ns kalai.pass.kalai.a-annotate-ast
  (:require [meander.strategy.epsilon :as s]
            [meander.epsilon :as m]
            [clojure.tools.analyzer.ast :as ast]
            [kalai.util :as u])
  (:import (clojure.lang IMeta)))

(def ref-vars
  #{#'atom
    #'ref
    #'agent})

(def ast-type
  (s/rewrite
    {:op  :const
     :val ?val}
    ~(type ?val)

    {:op   :invoke
     :fn   {:var (m/pred ref-vars)}
     :args [?value . _ ...]}
    ~(ast-type ?value)

    {:op   :with-meta
     :meta {:op   :map
            :form {:t ?t :tag ?tag}}
     :as   ?with-meta}
    ~(or ?t ?tag)

    {:o-tag ?o-tag}
    ?o-tag

    ?else
    nil))

(defn propagate-ast-type [from to]
  (if (and (instance? IMeta to)
           (not (u/type-from-meta to)))
    (u/maybe-meta-assoc to :t (ast-type from))
    to))

(defn resolve-t
  "Tools analyzer does not evaluate metadata in bindings,
  or arglists, so these need to be resolved."
  [x kaliases]
  (some-> (meta x)
          (:t)
          (kaliases)))

(defn substitute-aliased-types
  "Replace type aliases with their definition.
  Matches any AST node with meta data {:t T}
  where T is a var with meta {:kalias K}."
  [kaliases ast]
  (m/rewrite ast
    {:op   :def
     :name (m/and
             (m/app #(resolve-t % kaliases) (m/pred some? ?t))
             ?name)
     &     ?ast}
    ;;->
    {:op   :def
     :name ~(u/maybe-meta-assoc ?name :t ?t)
     &     ?ast}

    {:op   :binding
     :form (m/and
             (m/app #(resolve-t % kaliases) (m/pred some? ?t))
             ?form)
     &     ?ast}
    ;;->
    {:op   :binding
     :form ~(u/maybe-meta-assoc ?form :t ?t)
     &     ?ast}

    {:op   :fn-method
     :form ((m/and
              (m/app #(resolve-t % kaliases) (m/pred some? ?t))
              [& ?params])
            & ?body)
     &     ?ast}
    ;;->
    {:op   :fn-method
     :form (~(u/maybe-meta-assoc ?params :t ?t)
             & ?body)
     &     ?ast}

    ;; otherwise leave the ast as is
    ?else
    ?else))

(def propagate-types
  "We propagate type information which is stored in metadata
  from the the place where they are declared on a symbol
  to all future usages of that symbol in scope.
  When the type metadata is not provided and the type of the
  initial value is known, we use the type of the value."
  ;; TODO: function call type inference would be nice
  (s/rewrite
    {:op   :local
     :form ?symbol
     :env  {:locals {?symbol {:form ?symbol-with-meta
                              :init ?init}}
            :as     ?env}
     &     ?ast}
    ;;->
    {:op   :local
     :form ~(propagate-ast-type ?init ?symbol-with-meta)
     :env  ?env
     &     ?ast}

    ;; otherwise leave the ast as is
    ?else
    ?else))

(def extract-type-aliases
  "Takes a vector of ASTs,
  returns a vector pair of the map of kaliases and the non-kalias ASTs as a sequence."
  (s/rewrite
    [(m/or {:op   :def
            :meta {:form {:kalias (m/pred some? !kalias)}}
            :name !name}
           !ast) ...]
    ;;->
    [{& ([!name !kalias] ...)}
     (!ast ...)]

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

;; TODO: maybe rewrite this as a meander expression,
;; or at least clean up the excessive function hijinks
(defn substitute-and-erase-type-aliases [asts]
  (let [[kaliases asts] (extract-type-aliases asts)]
    (map #(ast/prewalk % (fn [x]
                           (substitute-aliased-types kaliases x)))
         asts)))

;; TODO: split this mini-pipeline into 3 passes under the ast folder
(defn rewrite
  "There is contextual information in the AST that is not available in s-expressions.
  The purpose of this pass is to capture that information and modify the s-expressions to contain what we need."
  [asts]
  (->> asts
       (substitute-and-erase-type-aliases)
       (map #(ast/prewalk % propagate-types))
       (map #(ast/prewalk % annotate-vars))))
