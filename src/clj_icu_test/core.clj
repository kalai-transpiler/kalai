(ns clj-icu-test.core
  (:require [clojure.edn :as edn]
            [clojure.string :as string]
            [clojure.tools.analyzer.jvm :as az]))

;;
;; constants
;;

(def INDENT-CHAR \space)

(def INDENT-LEVEL-INCREMENT 2)

(def LANGS {:java :cpp})

;;
;; indentation - global state and fns
;;

(def indent-level (atom 0))

(defn reset-indent-level [] (reset! indent-level 0))

(defmacro indent
  [& body] 
  `(do
     (swap! indent-level + INDENT-LEVEL-INCREMENT)
     (let [result# (do ~@body)]
       ;; force lazy sequences to evaluate now so that they use
       ;; the current state of the indent level
       (when (seq result#)
         (doall result#))
       (swap! indent-level - INDENT-LEVEL-INCREMENT) 
       result#)))

(defn indent-str-curr-level
  []
  (apply str (repeat @indent-level INDENT-CHAR)))

;;
;; records
;;

(defrecord AstOpts [ast env lang])

(defn new-ast-opts
  "keys of the opts map are the fields in AstOpts record"
  [opts]
  {:pre [(or (not (:lang opts))
             (get LANGS (:lang opts)))]}
  (map->AstOpts opts))

;;
;; C++
;;

(declare emit-cpp)

(defn emit-cpp-type
  [class]
  (when class
    (let [canonical-name (.getCanonicalName class)
          java-cpp-type-map {java.lang.Integer "int"
                             int "int"
                             java.lang.Long "long int"
                             long "long int"
                             java.lang.Float "float"
                             java.lang.Double "double float"
                             java.lang.Boolean "bool"
                             boolean "bool"
                             java.lang.String "string"}]
      (if-let [transformed-type (get java-cpp-type-map class)]
        transformed-type
        canonical-name))))

(defn emit-cpp-statement
  [statement-parts]
  (str (->> statement-parts
            (keep identity)
            (map str)
            (string/join " "))
       ";"))

(defn emit-cpp-const
  [ast-opts]
  {:pre [(= :const (:op (:ast ast-opts)))
         (:literal? (:ast ast-opts))]}  
  (let [ast (:ast ast-opts)]
    (pr-str (:val ast))))

;; bindings


(defn emit-cpp-assignment
  "To be used by both 'def' and any bindings block of a form (ex: let)
  Might return nil"
  [ast-opts]
  (let [ast (:ast ast-opts)
        op-code (:op ast)
        type-class (or (get-in ast [:meta :val :tag])
                       (get-in ast [:init :env :tag]))
        type-str (emit-cpp-type type-class)
        identifier (when-let [identifer-symbol (or (get-in ast [:env :form])
                                                   (case op-code
                                                     :binding (get ast :form)
                                                     :def (get ast :name)))]
                     (str identifer-symbol))
        expression (emit-cpp (assoc ast-opts :ast (:init ast)))
        statement-parts [type-str
                         identifier
                         "="
                         expression]
        statement (emit-cpp-statement statement-parts)]
    statement))

(defn emit-cpp-def
  [ast-opts]
  {:pre [(= :def (:op (:ast ast-opts)))]}
  (let [ast (:ast ast-opts)]
    (emit-cpp-assignment ast-opts)))

;; entry point

(defn emit-cpp
  [ast-opts]
  ;; TODO: some multimethod ?
  (let [ast (:ast ast-opts)]
    (case (:op ast)
      :def (emit-cpp-def ast-opts)
      :const (emit-cpp-const ast-opts)))
  )


;;
;; Java
;;

;; common forms

(declare emit-java)

(defn emit-java-type
  "Might return nil"
  [class]  
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
      (= Void/TYPE class) "void"
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
                (subs canonical-name 10)

                ;; this when condition prevents Clojure-specific (?) classes like "long",
                ;; "int", etc. that are automatically tagged by the analyzer on various
                ;; binding symbols from becoming included in the emitted output.  This
                ;; means that you need to used the boxed versions in type hints like
                ;; ^Long, ^Integer, etc. in order to create type signatures in the emitted
                ;; output.
                (when (.getPackage class))
                canonical-name

                :else
                nil)))))

(defn emit-java-statement
  "input is a seq of strings"
  [statement-parts]
  (str (indent-str-curr-level)
       (->> statement-parts
            (keep identity)
            (map str)
            (string/join " "))
       ";"))

(defn can-become-java-statement
  "input is a string representing a statement"
  [expression]
  (let [result
        (let [last-char (last expression)]
          (and (not= \; last-char)
               (not= \} last-char)))]
    result))

(defn emit-java-const
  [ast-opts]
  {:pre [(= :const (:op (:ast ast-opts)))
         (:literal? (:ast ast-opts))]}
  (let [ast (:ast ast-opts)]
    (pr-str (:val ast))))

(defn emit-java-do
  [ast-opts]
  {:pre [(= :do (:op (:ast ast-opts)))]}
  (let [ast (:ast ast-opts)
        stmts (:statements ast)
        stmts-ast-opts (map (partial assoc ast-opts :ast) stmts)
        stmt-emitted-lines (map emit-java stmts-ast-opts)
        last-stmt (:ret ast)
        last-emitted-line (emit-java (assoc ast-opts :ast last-stmt))
        all-lines (concat stmt-emitted-lines [last-emitted-line])]
    all-lines))

;; bindings

(defn emit-java-atom
  [ast-opts]
  {:pre [(and (= :invoke (:op (:ast ast-opts)))
              (= (symbol "atom") (-> ast-opts :ast :fn :meta :name)))]}
  (let [ast (:ast ast-opts)
        init-val-ast (-> ast
                         :args
                         first)]
    (emit-java (assoc ast-opts :ast init-val-ast))))

(defn emit-java-reset!
  [ast-opts]
  {:pre [(and (= :invoke (:op (:ast ast-opts)))
              (= (symbol "reset!") (-> ast-opts :ast :fn :meta :name)))]}
  (let [ast (:ast ast-opts)
        identifier (str (or (-> ast :args first :meta :name)
                            (-> ast :args first :form)))
        reset-val-ast (-> ast
                          :args
                          second)
        expression (emit-java (assoc ast-opts :ast reset-val-ast))
        statement-parts [identifier
                         "="
                         expression]
        statement (emit-java-statement statement-parts)]
    statement))

(defn emit-java-assignment
  "To be used by both 'def' and any bindings block of a form (ex: let)
  Might return nil"
  [ast-opts]
  (let [ast (:ast ast-opts)
        op-code (:op ast) 
        type-class (or (get-in ast [:meta :val :tag])
                       (get-in ast [:init :env :tag])
                       (and (= :binding op-code)
                            (get ast :tag)))        
        type-str (emit-java-type type-class)
        identifier (when-let [identifer-symbol (or (get-in ast [:env :form])
                                                   (case op-code
                                                     :binding (get ast :form)
                                                     :def (get ast :name)))]
                     (str identifer-symbol))
        expression (emit-java (assoc ast-opts :ast (:init ast)))
        statement-parts [type-str
                         identifier
                         "="
                         expression]        
        statement (emit-java-statement statement-parts)]
    statement))

(defn emit-java-def
  [ast-opts]
  {:pre [(= :def (:op (:ast ast-opts)))]}
  (let [ast (:ast ast-opts)]
    (emit-java-assignment ast-opts)))

(defn emit-java-binding
  "Might return nil"
  [ast-opts]
  {:pre [(= :binding (:op (:ast ast-opts)))]}
  (let [ast (:ast ast-opts)]
    (emit-java-assignment ast-opts)))

(defn emit-java-bindings-stanza
  "The bindings stanza in a form (ex: in a let form).
  Might return nil"
  [ast-opts]
  {:pre [(sequential? (:ast ast-opts))]}
  (let [ast (:ast ast-opts)
        bindings ast ;; the AST in this case is a vector of sub-ASTs
        bindings-ast-opts (map (partial assoc ast-opts :ast) bindings)
        binding-statements (map emit-java-binding bindings-ast-opts)]
    (when-let [non-nil-binding-statements (->> binding-statements
                                               (keep identity)
                                               seq)]
      (let [bindings-str (string/join "\n" non-nil-binding-statements)] 
        bindings-str))))

(defn emit-java-let
  [ast-opts]
  {:pre [(= :let (:op (:ast ast-opts)))]}
  (let [ast (:ast ast-opts)
        bindings (:bindings ast)
        binding-str (indent (emit-java-bindings-stanza (assoc ast-opts :ast bindings)))
        body-ast (:body ast)
        body-strs (indent
                   (if (:statements body-ast)
                     ;; if ast has key nesting of [:body :statements], then we have a multi-"statement" expression do block in the let form
                     (let [butlast-statements (:statements body-ast)
                           last-statement (:ret body-ast)
                           statements (concat butlast-statements [last-statement])
                           statement-ast-opts (map #(assoc ast-opts :ast %) statements)
                           statement-strs (map emit-java statement-ast-opts)]
                       statement-strs)
                     ;; else the let block has only one "statement" in the do block
                     [(emit-java (assoc ast-opts :ast body-ast))]))
        body-strs-with-semicolons (indent
                                   (map #(if-not (can-become-java-statement %)
                                           %
                                           (emit-java-statement [%]))
                                        body-strs))
        body-str (string/join "\n" body-strs-with-semicolons)
        block-str-parts [(str (indent-str-curr-level) "{")
                         binding-str
                         body-str
                         (str (indent-str-curr-level) "}")]        
        block-str (->> block-str-parts
                       (keep identity)
                       (string/join "\n"))] 
    block-str))

;; "arithmetic" (built-in operators)

(defn emit-java-arg
  [ast-opts symb]
  {:pre [(:env ast-opts)]}
  (let [ast-opts-env (:env ast-opts)
        symb-class (class symb)
        symb-ast (az/analyze symb ast-opts-env)
        symb-ast-opts {:ast symb-ast}]
    (cond

      ;; emit a "standalone" token
      (= clojure.lang.Symbol symb-class)
      (emit-java symb-ast-opts)
      
      ;; a seq of symbols -> emit parenthases around the emitted form
      (or (isa? symb-class clojure.lang.IPersistentCollection)
          (isa? symb-class clojure.lang.ISeq))
      (if (= 1 (count symb-ast))
        (emit-java (assoc symb-ast-opts :ast (first symb-ast)))
        (str "(" (emit-java symb-ast-opts) ")"))

      ;; else, we have something that we treat like a scalar
      :else (emit-java symb-ast-opts))))

(defn emit-java-args
  [ast-opts]
  {:pre [(-> ast-opts :ast :raw-forms seq)]}
  (let [ast (:ast ast-opts)
        raw-forms (-> ast :raw-forms)
        raw-form-arg-symbols (-> raw-forms
                                 last
                                 rest)
        raw-form-arg-symbol-ast-opts {:env (-> ast :env)}
        emitted-args (map (partial emit-java-arg raw-form-arg-symbol-ast-opts) raw-form-arg-symbols)]
    emitted-args))

(defn emit-java-static-call
  [ast-opts]
  {:pre [(= :static-call (:op (:ast ast-opts)))]}
  (let [ast (:ast ast-opts)
        static-call-fn-symbol (let [fn-symbol (-> ast :raw-forms last first)
                                    fn-str (str fn-symbol)]
                                (case  fn-str
                                  "quot" "/"
                                  "rem" "%"
                                  ;; Note: extra work is required if supporting Clojure expressions using =
                                  ;; with more than 2 expression arguments.  Not really high
                                  ;; priority at the moment to support > 2 args for =
                                  "=" "=="
                                  fn-str))
        arg-strs (emit-java-args ast-opts)
        expression-parts (interpose static-call-fn-symbol arg-strs)
        expression (string/join " " expression-parts)]
    expression))


;; other

(defn emit-java-local
  [ast-opts]
  {:pre [(= :local (:op (:ast ast-opts)))]}
  (let [ast (:ast ast-opts)
        form (:form ast)]
    (str (name form))))

(defn emit-java-var
  [ast-opts]
  {:pre [(= :var (:op (:ast ast-opts)))]}
  (let [ast (:ast ast-opts)
        form (:form ast)]
    (str (name form))))

;; functions

(defn emit-java-defn-arg
  [ast-opts]
  {:pre [(= :binding (-> ast-opts :ast :op))]}
  (let [ast (:ast ast-opts)
        arg-name (-> ast :form name)
        type-class (-> ast :tag)
        type-str (emit-java-type type-class)
        identifier-signature-parts [type-str
                                    arg-name]
        identifier-signature (->> identifier-signature-parts
                                  (keep identity)
                                  (string/join " "))]
    identifier-signature))

(defn emit-java-defn-args
  [ast-opts]
  ;; Note: can have empty args
  ;;{:pre [(seq (:ast ast-opts))]}
  (let [ast (:ast ast-opts)
        arg-ast-seq ast
        arg-ast-opts (->> arg-ast-seq
                          (map #(assoc ast-opts :ast %))
                          (map emit-java-defn-arg))]
    (string/join ", " arg-ast-opts)))

(defn emit-java-defn
  "currently does not handle variadic fns (fn overloading)"
  [ast-opts]
  {:pre [(= :def (-> ast-opts :ast :op))]}
  (let [ast (:ast ast-opts)
        fn-name (:name ast)
        fn-ast (:init ast)
        fn-return-type (-> fn-ast
                           :return-tag
                           emit-java-type)
        ;; Note: currently not dealing with fn overloading (variadic fns in Clojure),
        ;; so just take the first fn method
        fn-method-first (-> fn-ast
                            :expr
                            :methods first)
        fn-method-first-arg-asts (:params fn-method-first)
        fn-method-first-args (-> {:ast fn-method-first-arg-asts}
                                 emit-java-defn-args)
        ;; Note: hard-coding all methods as public.  Access modifiers are important
        ;; for 'encapsulation', which has a much lower priority and disputed value.
        fn-method-first-signature-parts ["public"
                                         fn-return-type
                                         (str fn-name "(" fn-method-first-args ")")]
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
                                           statement-strs (map emit-java statement-ast-opts)]
                                       statement-strs)
                                     ;; else the let block has only one "statement" in the do block
                                     [(emit-java (assoc ast-opts :ast fn-method-first-body-ast))]))
        fn-method-first-body-strs-with-semicolons (indent
                                                   (map #(if-not (can-become-java-statement %)
                                                           %
                                                           (emit-java-statement [%]))
                                                        fn-method-first-body-strs))
        fn-method-first-body-str (string/join "\n" fn-method-first-body-strs-with-semicolons)
        fn-method-first-str-parts [(str (indent-str-curr-level) fn-method-first-signature)
                                   (str (indent-str-curr-level) "{")
                                   fn-method-first-body-str
                                   (str (indent-str-curr-level) "}")]
        fn-method-first-str (->> fn-method-first-str-parts
                                 (keep identity)
                                 (string/join "\n"))]
    fn-method-first-str))

;; classes (or modules or namespaces)

(defn defclass
  "Create a cosmetic fn that allows us to organize the forms in body"
  [name & body]
  ;; Note: everything in body will get evaluated as a result of being arguments passed
  ;; to the function.  The fn creates a name to be used when searching the AST to detect
  ;; this semantic.
  ;; Note: this trick of defining semantics in the AST through custom fns should be able
  ;; to work even in a nested fashion, AFAICT
  ;; Note: the return value shouldn't matter since the code/S-expressions in Clojure may
  ;; not result in working Clojure code, let alone the best/most efficient code.
  ;; Note: originally tried using a pass-through macro or wrapping the body inside a do
  ;; form, but then the original macro nor the do block show up in the AST in an easily
  ;; recognizable way.  In the future, if a macro is truly needed, the combination of
  ;; a macro and a function give the full power of expression (not requiring name to
  ;; be of a certain type) while still being easy to recognize in the AST.
  nil)

(defn emit-java-defclass
  [ast-opts]
  {:pre [(= :invoke (-> ast-opts :ast :op))]}
  (let [ast (:ast ast-opts)
        ;; Currently assuming that the class-name is provided as String
        class-name (-> ast :args first :val)
        ;; Note: making all classes public b/c no reason to do otherwise currently,
        ;; see emit-java-defn for reasoning.
        class-signature-parts ["public"
                               "class"
                               class-name]
        class-signature (string/join " " class-signature-parts)
        class-form-asts (-> ast :args rest)
        class-form-ast-opts (map (partial assoc ast-opts :ast) class-form-asts)
        class-form-strs (indent
                         (map emit-java class-form-ast-opts))
        class-form-strs-with-semicolons (indent
                                         (map #(if-not (can-become-java-statement %)
                                                 %
                                                 (emit-java-statement [%]))
                                              class-form-strs))
        ;; Note: should have a blank line between top-level statements/blocks
        ;; in a class, so join with 2 newlines instead of just 1 like in a let block
        class-forms-str (string/join "\n\n" class-form-strs-with-semicolons)
        class-str-parts [(str (indent-str-curr-level) class-signature)
                         (str (indent-str-curr-level) "{")
                         class-forms-str
                         (str (indent-str-curr-level) "}")]
        class-str (->> class-str-parts
                       (keep identity)
                       (string/join "\n"))]
    class-str))

;; enum classes

(defn defenum-impl
  "This is the fn in the macro + fn trick described in defclass above to allow
  ease of use for users while still preserving retrievability of info in AST"
  [name & body]
  ;; Note: similar notes as those for defclass
  nil)

(defmacro defenum
  "This is the macro in the macro + fn trick.  See defenum-impl and defclass."
  [name & body]
  (let [quoted-field-names (map str body)]
    (list* 'defenum-impl name quoted-field-names)))

(defn emit-java-defenum
  [ast-opts]
  {:pre [(= :invoke (-> ast-opts :ast :op))]}
  (let [ast (:ast ast-opts)
        ;; Note: similar to defclass, currently assuming name is provided as string
        enum-name (-> ast :args first :val)
        enum-class-signature-parts ["public"
                                    "enum"
                                    enum-name]
        enum-class-signature (string/join " " enum-class-signature-parts)
        ;; Note: assuming that enums are only provided as field names,
        ;; and actual values associated with each field name are not provided
        enum-field-asts (-> ast :args rest)
        enum-field-ast-opts (map (partial assoc ast-opts :ast) enum-field-asts)
        enum-field-strs (map emit-java enum-field-ast-opts)
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

;; return statement

(defn return
  "A pass-through function that defines \"return\" as a form"
  [expr]
  expr)

(defn emit-java-return
  [ast-opts]
  {:pre [(= :invoke (-> ast-opts :ast :op))]}
  (let [ast (:ast ast-opts)
        expr-ast (-> ast :args first)
        expr-ast-opts (assoc ast-opts :ast expr-ast)
        expr-ast-str (emit-java expr-ast-opts)
        return-stmt-str (emit-java-statement ["return"
                                              expr-ast-str])]
    return-stmt-str))

;; deref

(defn emit-java-deref
  [ast-opts]
  {:pre [(= :invoke (-> ast-opts :ast :op))]}
  (let [ast (:ast ast-opts)
        ;; Note: assuming that there is only one arg to deref, which is the symbol (identifier)
        identifier-symbol (-> ast :args first :form)
        identifier-str (str identifier-symbol)]
    identifier-str))

;; not

(defn emit-java-not
  [ast-opts]
  {:pre [(= :invoke (-> ast-opts :ast :op))]}
  (let [ast (:ast ast-opts)
        ;; Note: assuming that not only has 1 arg
        arg-ast (-> ast :args first)
        arg-str (emit-java (assoc ast-opts :ast arg-ast))
        expr-str (str "!(" arg-str ")")]
    expr-str))

;; fn invocations

(defn emit-java-invoke-arg
  [ast-opts]
  (emit-java ast-opts))

(defn emit-java-invoke-args
  [ast-opts]
  (let [ast (:ast ast-opts)
        args-ast (:args ast)
        args-ast-opts (map #(assoc ast-opts :ast %) args-ast)
        emitted-args (map emit-java-invoke-arg args-ast-opts)]
    emitted-args))

(defn emit-java-str
  [ast-opts]
  (let [ast (:ast ast-opts)
        arg-strs (emit-java-invoke-args ast-opts)
        arg-append-strs (map #(str ".append(" % ")") arg-strs)
        expr-parts (concat ["new StringBuffer()"]
                           arg-append-strs
                           [".toString()"])
        expr (apply str expr-parts)]
    expr))

(defn emit-java-println
  "Emit the equivalent of printing out to std out. To support auto-casting to str, insert a \"\" before the other args"
  [ast-opts]
  (let [ast (:ast ast-opts)
        arg-strs (emit-java-invoke-args ast-opts)
        all-arg-strs (cons "\"\"" arg-strs)
        command-expr (str "System.out.println("
                          (apply str (interpose " + " all-arg-strs))
                          ")")]
    command-expr))

(defn fn-matches?
  "indicates whether the function AST's meta submap matches the fn with the given fully-qualified namespace string and name.
  Takes an AST and returns a boolean."
  [fn-meta-ast exp-ns-str exp-name-str]
  (let [fn-ns (:ns fn-meta-ast)
        fn-name (-> fn-meta-ast :name)
        exp-ns (find-ns (symbol exp-ns-str))]
    (boolean (and (= fn-ns exp-ns)
                  (= (name fn-name) exp-name-str)))))

(defn emit-java-invoke
  "handles invocations of known functions"
  [ast-opts]
  {:pre [(= :invoke (:op (:ast ast-opts)))]}
  (let [ast (:ast ast-opts)
        fn-meta-ast (-> ast :fn :meta)]
    (cond
      (fn-matches? fn-meta-ast "clojure.core" "deref")
      (emit-java-deref ast-opts)

      (fn-matches? fn-meta-ast "clojure.core" "str")
      (emit-java-str ast-opts)

      (fn-matches? fn-meta-ast "clojure.core" "not")
      (emit-java-not ast-opts)

      (fn-matches? fn-meta-ast "clojure.core" "println")
      (emit-java-println ast-opts)

      (fn-matches? fn-meta-ast "clj-icu-test.core" "defclass")
      (emit-java-defclass ast-opts)

      (fn-matches? fn-meta-ast "clj-icu-test.core" "defenum-impl")
      (emit-java-defenum ast-opts)

      (fn-matches? fn-meta-ast "clj-icu-test.core" "return")
      (emit-java-return ast-opts)
      
      )))

;; loops (ex: while, doseq)

(defn emit-java-while
  [ast-opts]
  {:pre [(= :loop (:op (:ast ast-opts)))]}
  (let [ast (:ast ast-opts)
        test-ast (-> ast :body :test)
        test-str (emit-java (assoc ast-opts :ast test-ast))
        then-ast (-> ast :body :then)
        ;; Note: we are ignoring the "return" expression stored in (-> ast :body :then :ret)
        ;; when we only look at (-> ast :body :then :statements)
        statements (:statements then-ast)
        body-strs (indent
                   (let [statement-ast-opts (map #(assoc ast-opts :ast %) statements)
                         statement-strs (map emit-java statement-ast-opts)]
                     statement-strs))
        body-strs-with-semicolons (indent
                                   (map #(if-not (can-become-java-statement %)
                                           %
                                           (emit-java-statement [%]))
                                        body-strs))
        body-str (string/join "\n" body-strs-with-semicolons)
        while-parts [(str (indent-str-curr-level) "while (" test-str ")")
                     (str (indent-str-curr-level) "{")
                     body-str
                     (str (indent-str-curr-level) "}")]
        while-str (string/join "\n" while-parts)]
    while-str))

(defn emit-java-loop
  [ast-opts]
  {:pre [(= :loop (:op (:ast ast-opts)))]}
  (let [ast (:ast ast-opts)
        form-symbol (-> ast :raw-forms first first)
        form-symbol-str (str form-symbol)]
    (case form-symbol-str
      "while" (emit-java-while ast-opts))))

;; entry point

(defn emit-java
  [ast-opts]
  (let [ast (:ast ast-opts)]
    ;; TODO: some multimethod ?
    (case (:op ast)
      :def (case (some-> ast :raw-forms last first name)
             "defn" (emit-java-defn ast-opts)
             (emit-java-def ast-opts))
      :const (emit-java-const ast-opts)
      :invoke (case (-> ast :fn :meta :name name)
                "atom" (emit-java-atom ast-opts)
                "reset!" (emit-java-reset! ast-opts)
                (emit-java-invoke ast-opts)) 
      :do (emit-java-do ast-opts)
      :let (emit-java-let ast-opts)
      :local (emit-java-local ast-opts)
      :static-call (emit-java-static-call ast-opts)
      :var (emit-java-var ast-opts)
      :loop (emit-java-loop ast-opts)
      :else (cond 
              (:raw-forms ast)
              (emit-java (assoc ast-opts :ast (-> ast :raw-forms)))))))
