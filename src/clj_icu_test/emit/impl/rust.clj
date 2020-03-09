(ns clj-icu-test.emit.impl.rust
  (:require [clj-icu-test.common :refer :all]
            [clj-icu-test.emit.interface :as iface :refer :all]
            [clj-icu-test.emit.langs :as l]
            [clj-icu-test.emit.impl.util.common-type-util :as common-type-util]
            [clj-icu-test.emit.impl.util.curlybrace-util :as cb-util]
            [clj-icu-test.emit.impl.util.rust-util :as rust-util]
            [clj-icu-test.emit.impl.util.rust-type-util :as rust-type-util]
            [clojure.edn :as edn]
            [clojure.string :as string]
            [clojure.tools.analyzer.jvm :as az])
  (:import [java.util List Map]))

(defmethod iface/emit-complex-type [::l/rust List]
  [ast-opts]
  (let [ast (:ast ast-opts)
        type-val (or (-> ast-opts :impl-state :type-class-ast :mtype)
                     (:mtype ast))]
    (let [type-parameter-val (second type-val)]
      (assert (sequential? type-parameter-val))
      (let [type-parameter-class-ast-opts (-> ast-opts
                                              (dissoc :ast)
                                              (assoc-in [:ast :mtype] type-parameter-val)
                                              (assoc-in [:impl-state :type-class-ast :mtype] type-parameter-val)
                                              )
            type-parameter (emit-type type-parameter-class-ast-opts) 
            type (str "Vec<" type-parameter ">")]
        type))))

(defmethod iface/emit-complex-type [::l/rust Map]
  [ast-opts]
  (let [ast (:ast ast-opts)
        type-val (:mtype ast)]
    (let [type-parameters-val (second type-val)]
      (assert (sequential? type-parameters-val))
      (let [map-key-type-parameter-val (first type-parameters-val)
            map-val-type-parameter-val (second type-parameters-val) 
            map-key-type-parameter-class-ast-opts (assoc-in ast-opts [:ast :mtype] map-key-type-parameter-val)
            map-val-type-parameter-class-ast-opts (assoc-in ast-opts [:ast :mtype] map-val-type-parameter-val) 
            map-key-type-parameter (emit-type map-key-type-parameter-class-ast-opts)
            map-val-type-parameter (emit-type map-val-type-parameter-class-ast-opts)
            type (str "HashMap<" map-key-type-parameter "," map-val-type-parameter ">")]
        type))))

(defmethod iface/emit-scalar-type ::l/rust
  [ast-opts]
  (let [ast (:ast ast-opts)
        class (or (:return-tag ast) 
                  (-> ast :impl-state :type-class-ast :mtype)
                  (:mtype ast)
                  (:tag ast))]
    (when class
      (cond
        ;; TODO: uncomment the primitive type class code unless and until we want to have
        ;; implicit type signatures applied for bindings in a let block.
        ;; Note: the problem is that the analyzer automatically infers the type of the
        ;; binding symbol in a let binding block in :tag and :o-tag even if there is no
        ;; type hint, whereas it doesn't do so in other places of binding (ex: def)
        ;; (= Long/TYPE class) "long"
        ;; (= Integer/TYPE class) "int"
        ;; (= Character/TYPE class) "char"
        ;; (= Boolean/TYPE class) "boolean"
        
        (= Void/TYPE class) nil ;; void return types for fns do not contribute to method type signature
        
        :else (let [canonical-name (.getCanonicalName class)] 
                (cond                
                  ;; this is to prevent the analyzer from auto-tagging the type classes of
                  ;; symbols in a binding form in a way that is currently being assumed to
                  ;; be unwanted in the emitted output.
                  ;; If we need to actually emit a type signature of Object in the future,
                  ;; we can subclass Object to a custom type (ex: ExplicitlyAnObject.java)
                  ;; and tell users to use that new class in their type hints if they want
                  ;; a type signature of java.lang.Object in emitted Java output.
                  (#{"java.lang.Object"
                     "java.lang.Number"} canonical-name)
                  nil

                  (.startsWith canonical-name "java.lang.")
                  (let [java-cpp-type-map {java.lang.Integer "i32"
                                           int "i32"
                                           java.lang.Long "i64"
                                           long "i64"
                                           java.lang.Float "f32"
                                           java.lang.Double "f64"
                                           java.lang.Boolean "bool"
                                           boolean "bool"
                                           java.lang.String "String"
                                           java.lang.Character "char"
                                           java.lang.StringBuffer "Vec<char>"}]
                    (when-let [transformed-type (get java-cpp-type-map class)]
                      transformed-type))

                  ;; this when condition prevents Clojure-specific (?) classes like "long",
                  ;; "int", etc. that are automatically tagged by the analyzer on various
                  ;; binding symbols from becoming included in the emitted output.  This
                  ;; means that you need to used the boxed versions in type hints like
                  ;; ^Long, ^Integer, etc. in order to create type signatures in the emitted
                  ;; output.
                  (when (.getPackage class))
                  canonical-name

                  :else
                  nil))))))

(defmethod iface/emit-assignment-scalar-type ::l/rust
  [ast-opts]
  (let [ast (:ast ast-opts)
        type-class-ast (get-assignment-type-class-ast ast-opts)
        complex-expr-opts (let [nested-expr-sub-expr-opts ast-opts
                                ;; expression is nested within AST's :init :expr when calling analyze-ns, and :op = :with-meta
                                ;; but calling analyze returns the expr in AST's :init with corresponding :op
                                assignment-init-expr-opts (if (-> ast-opts :ast :init :expr)
                                                            (-> ast-opts (update-in [:ast] (comp :expr :init)))
                                                            (-> ast-opts (update-in [:ast] :init)))
                                assignment-init-expr-with-type-opts (-> assignment-init-expr-opts 
                                                                        (assoc-in [:impl-state :type-class-ast] type-class-ast))]
                            (cond
                              (-> ast-opts :ast :init) assignment-init-expr-with-type-opts
                              :else nested-expr-sub-expr-opts))
        op-code (:op ast)
        type-class-opts (assoc ast-opts :ast type-class-ast) 
        type-str (emit-type type-class-opts)
        identifier (when-let [identifer-symbol (get-assignment-identifier-symbol ast-opts)]
                     (str identifer-symbol))
        expression (emit complex-expr-opts)
        is-atom (rust-util/is-assignment-expr-atom (:ast complex-expr-opts))
        is-immutable (not is-atom)
        identifier-and-type (if is-immutable
                              (if type-str
                                [(str "let " identifier ":") type-str]
                                [(str "let " identifier)])
                              (if type-str
                                [(str "let mut " identifier ":") type-str]
                                [(str "let mut " identifier)]))
        statement-parts (concat
                         identifier-and-type
                         ["="
                          expression])
        statement-parts-opts (-> ast-opts
                                 (assoc :val statement-parts)
                                 map->AnyValOpts)
        statement (emit-statement statement-parts-opts)]
    statement))

(defmethod iface/emit-const-scalar-type [::l/rust :string]
  [ast-opts]
  (let [str-val (-> ast-opts
                    :ast
                    :val)]
    (str "String::from(" (pr-str str-val) ")")))

(defmethod iface/emit-const-scalar-type [::l/curlybrace :nil]
  [ast-opts]
  "None")

(defmethod iface/emit-const-complex-type [::l/rust :vector]
  [ast-opts]
  {:pre [(is-complex-type? ast-opts)
         (= :vector (or (-> ast-opts :ast :type)
                        (-> ast-opts :ast :op)))]}
  (rust-type-util/rust-emit-const-vector-not-nested ast-opts))

(defmethod iface/emit-assignment-complex-type [::l/rust :vector]
  [ast-opts]
  {:pre [(or (and (= :const (-> ast-opts :ast :init :op))
                  (= :vector (-> ast-opts :ast :init :type)))
             (= :vector (-> ast-opts :ast :init :op))
             (= :vector (-> ast-opts :ast :type)))]}
  (let [ast (:ast ast-opts)
        type-class-ast (get-assignment-type-class-ast ast-opts)
        identifier (cond
                     (-> ast-opts :impl-state :identifier)
                     (-> ast-opts :impl-state :identifier)
                     
                     :else
                     (when-let [identifer-symbol (get-assignment-identifier-symbol ast-opts)]
                           (str identifer-symbol)))
        expr-ast-opts (if (-> ast-opts :ast :init)
                        (update-in ast-opts [:ast] :init)
                        ast-opts)]
    (if-not (common-type-util/is-const-vector-nested? expr-ast-opts)
      (rust-type-util/rust-emit-assignment-vector-not-nested ast-opts)
      (let [impl-state {:identifier identifier
                        :type-class-ast type-class-ast}
            expr-ast-opts-init-impl-state (update-in expr-ast-opts [:impl-state] merge impl-state)]
        (rust-type-util/rust-emit-assignment-vector-nested expr-ast-opts-init-impl-state)))))

(defmethod iface/emit-assignment-complex-type [::l/rust :map]
  [ast-opts]
  {:pre [(or (and (= :const (-> ast-opts :ast :init :op))
                  (= :map (-> ast-opts :ast :init :type)))
             (= :map (-> ast-opts :ast :init :op)))]}
  (let [ast (:ast ast-opts)
        type-class-ast (let [default-type-class-ast (get-assignment-type-class-ast ast-opts)
                             updated-type-class-ast (if (-> ast :form meta :mtype)
                                                      ;; *** this uses eval *** -- see curlybrace.clj 
                                                      (let [curr-ns (-> ast :env :ns find-ns)
                                                            metadata-form (-> ast :form meta)
                                                            metadata-val (binding [*ns* curr-ns]
                                                                           (eval metadata-form))]
                                                        (merge default-type-class-ast metadata-val))
                                                      default-type-class-ast)]
                         updated-type-class-ast)
        identifier (when-let [identifer-symbol (get-assignment-identifier-symbol ast-opts)]
                     (str identifer-symbol))
        expr-ast-opts (update-in ast-opts [:ast] :init)] 
    (if-not (common-type-util/is-const-map-nested? expr-ast-opts)
      (rust-type-util/rust-emit-assignment-map-not-nested ast-opts)
      (let [impl-state {:identifier identifier
                        :type-class-ast type-class-ast}
            expr-ast-opts-init-impl-state (assoc expr-ast-opts :impl-state impl-state)]
        (rust-type-util/rust-emit-assignment-map-nested expr-ast-opts-init-impl-state)))))

(defmethod iface/get-custom-emitter-scalar-types ::l/rust
  [ast-opts]
  #{:string :char :nil})

;; "arithmetic" (built-in operators)

(defmethod iface/emit-arg ::l/rust
  [ast-opts symb]
  (rust-util/emit-arg-val ast-opts symb))

(defmethod iface/emit-args ::l/rust
  [ast-opts]
  (rust-util/emit-args-val ast-opts))

;; other built-in fns (also marked with op = :static-call)

(defmethod iface/emit-get ::l/rust
  [ast-opts]
  {:pre [(= :static-call (:op (:ast ast-opts)))
         (= "get" (-> ast-opts :ast :raw-forms last first str))]}
  (let [ast (:ast ast-opts)
        args (:args ast)
        arg-strs (rust-util/emit-args-ref ast-opts)
        data-structure-name-str (first arg-strs)
        key-str (second arg-strs)
        expr-parts ["*"
                    data-structure-name-str
                    ".get("
                    key-str
                    ").unwrap()"]
        expr (apply str expr-parts)]
    expr))

(defmethod iface/emit-nth ::l/rust
  [ast-opts]
  {:pre [(= :static-call (:op (:ast ast-opts)))
         (= "nth" (-> ast-opts :ast :raw-forms last first str))]}
  (let [ast (:ast ast-opts)
        args (:args ast)
        arg-strs (emit-args ast-opts)
        data-structure-name-str (first arg-strs)
        key-str (second arg-strs)
        expr-parts [data-structure-name-str
                    "["
                    key-str
                    " as usize]"]
        expr (apply str expr-parts)]
    expr))

;; defn (functions)

(defmethod iface/emit-defn-arg ::l/rust
  [ast-opts]
  {:pre [(= :binding (-> ast-opts :ast :op))]}
  (let [ast (:ast ast-opts)
        arg-name (-> ast :form name) 
        type-class-opts ast-opts 
        type-str (emit-type type-class-opts)
        type-prefix-pass-by-ref (if (rust-util/defn-arg-as-reference? ast-opts)
                                  "&"
                                  "")
        identifier-signature-parts [(str arg-name ":")
                                    (str type-prefix-pass-by-ref
                                         type-str)]
        identifier-signature (->> identifier-signature-parts
                                  (keep identity)
                                  (string/join " "))]
    identifier-signature))

(defmethod iface/emit-defn ::l/rust
  [ast-opts]
  {:pre [(= :def (-> ast-opts :ast :op))]}
  (let [ast (:ast ast-opts)
        fn-name (:name ast)
        fn-ast (:init ast) 
        fn-ast-opts (assoc ast-opts :ast fn-ast)
        fn-return-type (if (-> ast :arglists first meta :mtype) 
                         ;; *** this uses eval *** -- see curlybrace.clj
                         (let [curr-ns (-> fn-ast :env :ns find-ns)
                               metadata-form (-> ast :arglists first meta)
                               metadata-val (binding [*ns* curr-ns]
                                              (eval metadata-form))
                               fn-return-type-opts (map->AstOpts {:ast metadata-val :lang (:lang ast-opts)})
                               return-type (emit-type fn-return-type-opts)]
                           return-type) 
                         (emit-type fn-ast-opts))
        ;; Note: currently not dealing with fn overloading (variadic fns in Clojure),
        ;; so just take the first fn method
        fn-method-first (-> fn-ast
                            :expr
                            :methods first)
        fn-method-first-arg-asts (:params fn-method-first)
        fn-method-first-args (-> (assoc ast-opts :ast fn-method-first-arg-asts)
                                 emit-defn-args)
        ;; Note: excluding the "public" access modifier that you would see in Java b/c
        ;; I assume that this happens normally in C++, at least for methods
        fn-method-first-signature-parts ["pub"
                                         "fn"
                                         (str fn-name "(" fn-method-first-args ")")
                                         (when fn-return-type
                                           "->")
                                         fn-return-type]
        fn-method-first-signature (->> fn-method-first-signature-parts
                                       (keep identity)
                                       (string/join " " ))
        fn-method-first-body-ast (:body fn-method-first)
        fn-method-first-body-strs (indent
                                   (if (:statements fn-method-first-body-ast)
                                     ;; if ast has key nesting of [:body :statements], then we have a multi-"statement" expression do block in the let form
                                     (let [butlast-statements (:statements fn-method-first-body-ast)
                                           last-statement (:ret fn-method-first-body-ast)
                                           statements (concat butlast-statements [last-statement])
                                           statement-ast-opts (map #(assoc ast-opts :ast %) statements)
                                           statement-strs (map emit statement-ast-opts)]
                                       statement-strs)
                                     ;; else the let block has only one "statement" in the do block
                                     [(emit (assoc ast-opts :ast fn-method-first-body-ast))]))
        fn-method-first-body-strs-opts-seq (map #(-> ast-opts
                                                     (assoc :val %)
                                                     map->AnyValOpts)
                                                fn-method-first-body-strs) 
        fn-method-first-body-strs-with-semicolons (indent
                                                   (map #(if-not (can-become-statement %)
                                                           (:val %)
                                                           (emit-statement %))
                                                        fn-method-first-body-strs-opts-seq))
        fn-method-first-body-str (string/join "\n" fn-method-first-body-strs-with-semicolons)
        fn-method-first-str-parts [(str (indent-str-curr-level) fn-method-first-signature)
                                   (str (indent-str-curr-level) "{")
                                   fn-method-first-body-str
                                   (str (indent-str-curr-level) "}")]
        fn-method-first-str (->> fn-method-first-str-parts
                                 (keep identity)
                                 (string/join "\n"))]
    fn-method-first-str))

(defmethod iface/emit-contains? ::l/rust
  [ast-opts]
  (let [val-arg-strs (rust-util/emit-args-val ast-opts)
        ref-arg-strs (rust-util/emit-args-ref ast-opts)
        coll-name-str (first val-arg-strs)
        key-str (second ref-arg-strs)
        expr-parts [coll-name-str
                    ".contains_key("
                    key-str
                    ")"]
        expr (apply str expr-parts)]
    expr))

(defmethod iface/emit-str-arg ::l/rust
  [ast-opts]
  (let [ast (:ast ast-opts)
        tag-class (:tag ast)
        emitted-arg (emit ast-opts)
        tag-class-opts (-> ast-opts
                           (assoc :val tag-class)
                           map->AnyValOpts)
        casted-emitted-arg (if (is-number-type? tag-class-opts)
                             (str "(" emitted-arg ").to_string()") 
                             emitted-arg)]
    casted-emitted-arg))

(defmethod iface/emit-str-args ::l/rust
  [ast-opts]
  (let [ast (:ast ast-opts)
        args-ast (:args ast)
        args-ast-opts (map #(assoc ast-opts :ast %) args-ast)
        emitted-args (map emit-str-arg args-ast-opts)]
    emitted-args))

(defmethod iface/emit-str ::l/rust
  [ast-opts]
  (let [ast (:ast ast-opts)
        arg-strs (emit-str-args ast-opts)
        format-first-arg (->> (concat ["\""]
                                      (-> (count arg-strs)
                                          (repeat "{}"))
                                      ["\""])
                              (apply str))
        format-all-args (concat [format-first-arg]
                                arg-strs)
        format-arg-expr (string/join ", " format-all-args)
        format-expr-parts ["format!("
                           format-arg-expr
                           ")"]
        expr (apply str format-expr-parts)]
    expr))

(defmethod iface/emit-println ::l/rust
  [ast-opts]
  (let [ast (:ast ast-opts)
        arg-str (emit-str ast-opts)
        all-arg-strs ["println!(\"{}\", "
                      arg-str
                      ")"]
        command-expr (apply str all-arg-strs)]
    command-expr))

;; classes (or modules or namespaces)

(defmethod iface/emit-defclass ::l/rust
  [ast-opts]
  {:pre [(= :invoke (-> ast-opts :ast :op))]}
  (let [ast (:ast ast-opts)
        ;; Currently ignoring classes because assuming it as a
        ;; construct that doesn't translate to Rust

        class-form-asts (-> ast :args rest)
        class-form-ast-opts (map (partial assoc ast-opts :ast) class-form-asts)
        class-form-strs (map emit class-form-ast-opts)
        class-form-strs-opts-seq (map #(-> ast-opts
                                           (assoc :val %)
                                           map->AnyValOpts)
                                      class-form-strs) 
        class-form-strs-with-semicolons (map #(if-not (can-become-statement %)
                                                (:val %)
                                                (emit-statement %))
                                             class-form-strs-opts-seq)
        ;; Note: should have a blank line between top-level statements/blocks
        ;; in a class, so join with 2 newlines instead of just 1 like in a let block
        class-forms-str (string/join "\n\n" class-form-strs-with-semicolons)
        class-str-parts [class-forms-str]
        class-str (->> class-str-parts
                       (keep identity)
                       (string/join "\n"))]
    class-str))

;; enum classes

(defmethod iface/emit-defenum ::l/rust
  [ast-opts]
  {:pre [(= :invoke (-> ast-opts :ast :op))]}
  (let [ast (:ast ast-opts)
        ;; Note: similar to defclass, currently assuming name is provided as string
        enum-name (-> ast :args first :val)
        enum-class-signature-parts ["enum"
                                    enum-name]
        enum-class-signature (string/join " " enum-class-signature-parts)
        ;; Note: assuming that enums are only provided as field names,
        ;; and actual values associated with each field name are not provided
        enum-field-asts (-> ast :args rest)
        enum-field-strs (map :val enum-field-asts)
        enum-field-unescaped-strs (map edn/read-string enum-field-strs)
        enum-field-symbols (map symbol enum-field-unescaped-strs)
        enum-field-indented-symbols (indent
                                     (map #(str (indent-str-curr-level) %) enum-field-symbols))
        enum-field-strs-with-commas (concat (->> enum-field-indented-symbols
                                                 butlast
                                                 (map #(str % ",")))
                                            [(last enum-field-indented-symbols)])
        enum-fields-str (string/join "\n" enum-field-strs-with-commas)
        enum-class-str-parts [(str (indent-str-curr-level) enum-class-signature)
                              (str (indent-str-curr-level) "{")
                              enum-fields-str
                              (str (indent-str-curr-level) "}")]
        enum-class-str (->> enum-class-str-parts
                            (keep identity)
                            (string/join "\n"))]
    enum-class-str))

;; fn invocations

(defmethod iface/emit-new-strbuf ::l/rust
  [ast-opts]
  ;; Note: currently assuming that there are 0 args to StringBuffer,
  ;; but can support args later
  (let [ast (:ast ast-opts)
        args (:args ast)]
    (if (zero? (count args))
      "String::new().chars().collect()"
      (let [arg-strs (emit-invoke-args ast-opts)
            init-val-str (first arg-strs)]
        (str init-val-str ".chars().collect()")))))

(defmethod iface/emit-prepend-strbuf ::l/rust
  [ast-opts]
  (let [ast (:ast ast-opts)
        args (:args ast)
        arg-strs (emit-invoke-args ast-opts)
        obj-name (first arg-strs)
        inserted-val-ast (nth args 1)
        inserted-val-str (-> inserted-val-ast
                             :val)

        ;; TODO: refactor the effort to create a let binding expression

        ;; if this emitter method was triggered by a form like (prepend-strbuf sb "hello"),
        ;; then we want to have the Rust equivalent of a local (let block) binding
        ;; that would look like (let [^StringBuffer sbTemp (new-strbuf "hello")] ...),
        ;; but just take the Rust equivalent of only the binding that we care about.
        ;;
        ;; Use 'str' as the fn and then replace it with new-strbuf b/c the analyzed
        ;; form when using new-strbuf as the fn would falsely introduce a nil arg
        ;; at the beginning of the args list.
        ;; Use a dummy arg and then replace its AST with the AST of the value we want.
        temp-obj-name (-> (str obj-name "_temp")
                          cb-util/new-name)
        temp-obj-symbol (with-meta (symbol temp-obj-name) {:tag StringBuffer})
        temp-obj-ast (az/analyze `(let [~temp-obj-symbol (atom (str "replaceme"))]))
        new-strbuf-fn-ast (-> (az/analyze '(new-strbuf))
                              :fn)
        temp-obj-binding-ast (-> temp-obj-ast :bindings
                                 (assoc-in [0 :init :args 0 :fn] new-strbuf-fn-ast)
                                 (assoc-in [0 :init :args 0 :args 0] inserted-val-ast))        
        temp-obj-binding-ast-opts (assoc ast-opts :ast temp-obj-binding-ast)
        temp-obj-binding-str (emit-bindings-stanza temp-obj-binding-ast-opts)
        
        insert-invoke-parts [obj-name
                             ".splice(0..0, "
                             temp-obj-name
                             ")"]
        insert-invoke (apply str insert-invoke-parts)

        statements [temp-obj-binding-str
                    insert-invoke]
        statements-val-opts (map->AnyValOpts (assoc ast-opts :val statements))
        result (emit-statements statements-val-opts)]
    result))

(defmethod iface/emit-insert-strbuf-char ::l/rust
  [ast-opts]
  (let [ast (:ast ast-opts)
        args (:args ast)
        arg-strs (emit-invoke-args ast-opts)
        obj-name (first arg-strs)
        idx (nth arg-strs 1)
        inserted-val-str (nth arg-strs 2)
        insert-invoke-parts [obj-name
                              ".insert("
                              idx
                              ", "
                              inserted-val-str
                              ")"]
        insert-invoke (apply str insert-invoke-parts)]
    insert-invoke))

(defmethod iface/emit-insert-strbuf-string ::l/rust
  [ast-opts]
  (let [ast (:ast ast-opts)
        args (:args ast)
        arg-strs (emit-invoke-args ast-opts)
        obj-name (first arg-strs)
        idx (nth arg-strs 1)
        inserted-val-ast (nth args 2)
        inserted-val-str (-> inserted-val-ast
                             :val)

        ;; TODO: refactor the effort to create a let binding expression

        ;; if this emitter method was triggered by a form like (insert-strbuf-string sb 0 "hello"),
        ;; then we want to have the Rust equivalent of a local (let block) binding
        ;; that would look like (let [^StringBuffer sbTemp (new-strbuf "hello")] ...),
        ;; but just take the Rust equivalent of only the binding that we care about.
        ;;
        ;; Use 'str' as the fn and then replace it with new-strbuf b/c the analyzed
        ;; form when using new-strbuf as the fn would falsely introduce a nil arg
        ;; at the beginning of the args list.
        ;; Use a dummy arg and then replace its AST with the AST of the value we want.
        temp-obj-name (-> (str obj-name "_temp")
                          cb-util/new-name)
        temp-obj-symbol (with-meta (symbol temp-obj-name) {:tag StringBuffer})
        temp-obj-ast (az/analyze `(let [~temp-obj-symbol (atom (str "replaceme"))]))
        new-strbuf-fn-ast (-> (az/analyze '(new-strbuf))
                              :fn)
        temp-obj-binding-ast (-> temp-obj-ast :bindings
                                 (assoc-in [0 :init :args 0 :fn] new-strbuf-fn-ast)
                                 (assoc-in [0 :init :args 0 :args 0] inserted-val-ast))        
        temp-obj-binding-ast-opts (assoc ast-opts :ast temp-obj-binding-ast)
        temp-obj-binding-str (emit-bindings-stanza temp-obj-binding-ast-opts)
        
        insert-invoke-parts [obj-name
                             ".splice("
                             idx
                             ".."
                             idx
                             ", "
                             temp-obj-name
                             ")"]
        insert-invoke (apply str insert-invoke-parts)

        statements [temp-obj-binding-str
                    insert-invoke]
        statements-val-opts (map->AnyValOpts (assoc ast-opts :val statements))
        result (emit-statements statements-val-opts)]
    result))

(defmethod iface/emit-tostring-strbuf ::l/rust 
  [ast-opts]
  (let [ast (:ast ast-opts)
        args (:args ast)
        arg-strs (emit-invoke-args ast-opts)
        obj-name (first arg-strs)
        tostring-str (str obj-name
                          ".into_iter().collect()")]
    tostring-str))

(defmethod iface/emit-length-strbuf ::l/rust
  [ast-opts]
  (let [ast (:ast ast-opts)
        args (:args ast)
        arg-strs (emit-invoke-args ast-opts)
        obj-name (first arg-strs)
        strlen-invoke (str obj-name ".len()")]
    strlen-invoke))

(defmethod iface/emit-strlen ::l/rust
  [ast-opts]
  (let [ast (:ast ast-opts)
        args (:args ast)
        arg-strs (emit-invoke-args ast-opts)
        obj-name (first arg-strs)
        strlen-invoke (str obj-name ".len()")]
    strlen-invoke))

(defmethod iface/emit-str-eq ::l/rust
  [ast-opts]
  (let [ast (:ast ast-opts)
        args (:args ast)
        arg-strs (emit-invoke-args ast-opts)
        obj-name-1 (first arg-strs)
        obj-name-2 (second arg-strs)
        strlen-invoke (str obj-name-1 " == " obj-name-2)]
    strlen-invoke))

(defmethod iface/emit-str-char-at ::l/rust
  [ast-opts]
  (let [ast (:ast ast-opts)
        args (:args ast)
        arg-strs (emit-invoke-args ast-opts)
        obj-name (first arg-strs)
        idx (second arg-strs)
        strlen-invoke (str obj-name ".chars().nth(" idx ").unwrap()")]
    strlen-invoke))

(defmethod iface/emit-seq-length ::l/rust
  [ast-opts]
  (let [ast (:ast ast-opts)
        args (:args ast)
        arg-strs (emit-invoke-args ast-opts)
        obj-name (first arg-strs)
        seq-length-invoke (str obj-name ".len()")]
    seq-length-invoke))

(defmethod iface/emit-seq-append ::l/rust
  [ast-opts]
  (let [ast (:ast ast-opts)
        args (:args ast)
        arg-strs (emit-invoke-args ast-opts)
        obj-name (first arg-strs)
        elem (second arg-strs)
        seq-append-invoke (str obj-name ".push(" elem ")")]
    seq-append-invoke))

;; new

(defmethod iface/emit-new ::l/rust
  [ast-opts]
  {:pre [(= :new (:op (:ast ast-opts)))]}
  (let [ast (:ast ast-opts)
        new-class-name (-> ast :class :form)
        ;; reuse invoke-args helper fns here
        arg-strs (emit-invoke-args ast-opts)
        arg-str-with-parens (let [arg-str (string/join ", " arg-strs)]
                              (str "(" arg-str ")"))
        new-str (->> [new-class-name
                      arg-str-with-parens]
                     (keep identity)
                     (apply str))]
    new-str))
