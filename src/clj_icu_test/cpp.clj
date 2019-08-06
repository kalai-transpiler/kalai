(ns clj-icu-test.cpp
  (:require [clj-icu-test.core :refer :all]
            [clojure.edn :as edn]
            [clojure.string :as string]
            [clojure.tools.analyzer.jvm :as az]))

;;
;; C++
;;

;; common forms

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


(defn emit-cpp-type
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
                (let [java-cpp-type-map {java.lang.Integer "int"
                                         int "int"
                                         java.lang.Long "long int"
                                         long "long int"
                                         java.lang.Float "float"
                                         java.lang.Double "double float"
                                         java.lang.Boolean "bool"
                                         boolean "bool"
                                         java.lang.String "string"}]
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
                nil)))))

(defn is-number-type?
  [class]
  (when class
    (let [number-classes #{java.lang.Number
                           java.lang.Short
                           java.lang.Integer
                           java.lang.Long
                           java.lang.Float
                           java.lang.Double}
          is-number-type (boolean
                          (get number-classes class))]
      is-number-type)))

(defn emit-cpp-statement
  [statement-parts]
  (str (indent-str-curr-level)
       (->> statement-parts
            (keep identity)
            (map str)
            (string/join " "))
       ";"))

(defn can-become-cpp-statement
  "input is a string representing a statement"
  [expression]
  (let [result
        (let [last-char (last expression)]
          (and (not= \; last-char)
               (not= \} last-char)))]
    result))

(defn emit-cpp-const
  [ast-opts]
  {:pre [(= :const (:op (:ast ast-opts)))
         (:literal? (:ast ast-opts))]}
  (let [ast (:ast ast-opts)]
    (pr-str (:val ast))))

(defn emit-cpp-do
  [ast-opts]
  {:pre [(= :do (:op (:ast ast-opts)))]}
  (let [ast (:ast ast-opts)
        stmts (:statements ast)
        stmts-ast-opts (map (partial assoc ast-opts :ast) stmts)
        stmt-emitted-lines (map emit-cpp stmts-ast-opts)
        last-stmt (:ret ast)
        last-emitted-line (emit-cpp (assoc ast-opts :ast last-stmt))
        all-lines (concat stmt-emitted-lines [last-emitted-line])]
    all-lines))

;; bindings

(defn emit-cpp-atom
  [ast-opts]
  {:pre [(and (= :invoke (:op (:ast ast-opts)))
              (= (symbol "atom") (-> ast-opts :ast :fn :meta :name)))]}
  (let [ast (:ast ast-opts)
        init-val-ast (-> ast
                         :args
                         first)]
    (emit-cpp (assoc ast-opts :ast init-val-ast))))

(defn emit-cpp-reset!
  [ast-opts]
  {:pre [(and (= :invoke (:op (:ast ast-opts)))
              (= (symbol "reset!") (-> ast-opts :ast :fn :meta :name)))]}
  (let [ast (:ast ast-opts)
        identifier (str (or (-> ast :args first :meta :name)
                            (-> ast :args first :form)))
        reset-val-ast (-> ast
                          :args
                          second)
        expression (emit-cpp (assoc ast-opts :ast reset-val-ast))
        statement-parts [identifier
                         "="
                         expression]
        statement (emit-cpp-statement statement-parts)]
    statement))

(defn emit-cpp-assignment
  "To be used by both 'def' and any bindings block of a form (ex: let)
  Might return nil"
  [ast-opts]
  (let [ast (:ast ast-opts)
        op-code (:op ast) 
        type-class (or (get-in ast [:meta :val :tag])
                       (get-in ast [:init :env :tag])
                       (and (= :binding op-code)
                            (get ast :tag)))        
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

(defn emit-cpp-binding
  "Might return nil"
  [ast-opts]
  {:pre [(= :binding (:op (:ast ast-opts)))]}
  (let [ast (:ast ast-opts)]
    (emit-cpp-assignment ast-opts)))

(defn emit-cpp-bindings-stanza
  "The bindings stanza in a form (ex: in a let form).
  Might return nil"
  [ast-opts]
  {:pre [(sequential? (:ast ast-opts))]}
  (let [ast (:ast ast-opts)
        bindings ast ;; the AST in this case is a vector of sub-ASTs
        bindings-ast-opts (map (partial assoc ast-opts :ast) bindings)
        binding-statements (map emit-cpp-binding bindings-ast-opts)]
    (when-let [non-nil-binding-statements (->> binding-statements
                                               (keep identity)
                                               seq)]
      (let [bindings-str (string/join "\n" non-nil-binding-statements)] 
        bindings-str))))

(defn emit-cpp-let
  [ast-opts]
  {:pre [(= :let (:op (:ast ast-opts)))]}
  (let [ast (:ast ast-opts)
        bindings (:bindings ast)
        binding-str (indent (emit-cpp-bindings-stanza (assoc ast-opts :ast bindings)))
        body-ast (:body ast)
        body-strs (indent
                   (if (:statements body-ast)
                     ;; if ast has key nesting of [:body :statements], then we have a multi-"statement" expression do block in the let form
                     (let [butlast-statements (:statements body-ast)
                           last-statement (:ret body-ast)
                           statements (concat butlast-statements [last-statement])
                           statement-ast-opts (map #(assoc ast-opts :ast %) statements)
                           statement-strs (map emit-cpp statement-ast-opts)]
                       statement-strs)
                     ;; else the let block has only one "statement" in the do block
                     [(emit-cpp (assoc ast-opts :ast body-ast))]))
        body-strs-with-semicolons (indent
                                   (map #(if-not (can-become-cpp-statement %)
                                           %
                                           (emit-cpp-statement [%]))
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

(defn emit-cpp-arg
  [ast-opts symb]
  {:pre [(:env ast-opts)]}
  (let [ast-opts-env (:env ast-opts)
        symb-class (class symb)
        symb-ast (az/analyze symb ast-opts-env)
        symb-ast-opts {:ast symb-ast}]
    (cond

      ;; emit a "standalone" token
      (= clojure.lang.Symbol symb-class)
      (emit-cpp symb-ast-opts)
      
      ;; a seq of symbols -> emit parenthases around the emitted form
      (or (isa? symb-class clojure.lang.IPersistentCollection)
          (isa? symb-class clojure.lang.ISeq))
      (if (= 1 (count symb-ast))
        (emit-cpp (assoc symb-ast-opts :ast (first symb-ast)))
        (str "(" (emit-cpp symb-ast-opts) ")"))

      ;; else, we have something that we treat like a scalar
      :else (emit-cpp symb-ast-opts))))

(defn emit-cpp-args
  [ast-opts]
  {:pre [(-> ast-opts :ast :raw-forms seq)]}
  (let [ast (:ast ast-opts)
        raw-forms (-> ast :raw-forms)
        raw-form-arg-symbols (-> raw-forms
                                 last
                                 rest)
        raw-form-arg-symbol-ast-opts {:env (-> ast :env)}
        emitted-args (map (partial emit-cpp-arg raw-form-arg-symbol-ast-opts) raw-form-arg-symbols)]
    emitted-args))

(defn emit-cpp-static-call
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
        arg-strs (emit-cpp-args ast-opts)
        expression-parts (interpose static-call-fn-symbol arg-strs)
        expression (string/join " " expression-parts)]
    expression))


;; other

(defn emit-cpp-local
  [ast-opts]
  {:pre [(= :local (:op (:ast ast-opts)))]}
  (let [ast (:ast ast-opts)
        form (:form ast)]
    (str (name form))))

(defn emit-cpp-var
  [ast-opts]
  {:pre [(= :var (:op (:ast ast-opts)))]}
  (let [ast (:ast ast-opts)
        form (:form ast)]
    (str (name form))))

;; functions

(defn emit-cpp-defn-arg
  [ast-opts]
  {:pre [(= :binding (-> ast-opts :ast :op))]}
  (let [ast (:ast ast-opts)
        arg-name (-> ast :form name)
        type-class (-> ast :tag)
        type-str (emit-cpp-type type-class)
        identifier-signature-parts [type-str
                                    arg-name]
        identifier-signature (->> identifier-signature-parts
                                  (keep identity)
                                  (string/join " "))]
    identifier-signature))

(defn emit-cpp-defn-args
  [ast-opts]
  ;; Note: can have empty args
  ;;{:pre [(seq (:ast ast-opts))]}
  (let [ast (:ast ast-opts)
        arg-ast-seq ast
        arg-ast-opts (->> arg-ast-seq
                          (map #(assoc ast-opts :ast %))
                          (map emit-cpp-defn-arg))]
    (string/join ", " arg-ast-opts)))

(defn emit-cpp-defn
  "currently does not handle variadic fns (fn overloading)"
  [ast-opts]
  {:pre [(= :def (-> ast-opts :ast :op))]}
  (let [ast (:ast ast-opts)
        fn-name (:name ast)
        fn-ast (:init ast)
        fn-return-type (-> fn-ast
                           :return-tag
                           emit-cpp-type)
        ;; Note: currently not dealing with fn overloading (variadic fns in Clojure),
        ;; so just take the first fn method
        fn-method-first (-> fn-ast
                            :expr
                            :methods first)
        fn-method-first-arg-asts (:params fn-method-first)
        fn-method-first-args (-> {:ast fn-method-first-arg-asts}
                                 emit-cpp-defn-args)
        ;; Note: excluding the "public" access modifier that you would see in Java b/c
        ;; I assume that this happens normally in C++, at least for methods
        fn-method-first-signature-parts [fn-return-type
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
                                           statement-strs (map emit-cpp statement-ast-opts)]
                                       statement-strs)
                                     ;; else the let block has only one "statement" in the do block
                                     [(emit-cpp (assoc ast-opts :ast fn-method-first-body-ast))]))
        fn-method-first-body-strs-with-semicolons (indent
                                                   (map #(if-not (can-become-cpp-statement %)
                                                           %
                                                           (emit-cpp-statement [%]))
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

(defn emit-cpp-defclass
  [ast-opts]
  {:pre [(= :invoke (-> ast-opts :ast :op))]}
  (let [ast (:ast ast-opts)
        ;; Currently assuming that the class-name is provided as String
        class-name (-> ast :args first :val)
        ;; Note: making all classes public b/c no reason to do otherwise currently,
        ;; see emit-cpp-defn for reasoning.
        class-signature-parts ["class"
                               class-name]
        class-signature (string/join " " class-signature-parts)
        class-form-asts (-> ast :args rest)
        class-form-ast-opts (map (partial assoc ast-opts :ast) class-form-asts)
        class-form-strs (indent
                         (map emit-cpp class-form-ast-opts))
        class-form-strs-with-semicolons (indent
                                         (map #(if-not (can-become-cpp-statement %)
                                                 %
                                                 (emit-cpp-statement [%]))
                                              class-form-strs))
        ;; Note: should have a blank line between top-level statements/blocks
        ;; in a class, so join with 2 newlines instead of just 1 like in a let block
        class-forms-str (string/join "\n\n" class-form-strs-with-semicolons)
        class-str-parts [(str (indent-str-curr-level) class-signature)
                         (str (indent-str-curr-level) "{")
                         class-forms-str
                         (str (indent-str-curr-level) "};")]
        class-str (->> class-str-parts
                       (keep identity)
                       (string/join "\n"))]
    class-str))

;; enum classes

(defn emit-cpp-defenum
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
        enum-field-ast-opts (map (partial assoc ast-opts :ast) enum-field-asts)
        enum-field-strs (map emit-cpp enum-field-ast-opts)
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
                              (str (indent-str-curr-level) "};")]
        enum-class-str (->> enum-class-str-parts
                            (keep identity)
                            (string/join "\n"))]
    enum-class-str))

;; return statement

(defn emit-cpp-return
  [ast-opts]
  {:pre [(= :invoke (-> ast-opts :ast :op))]}
  (let [ast (:ast ast-opts)
        expr-ast (-> ast :args first)
        expr-ast-opts (assoc ast-opts :ast expr-ast)
        expr-ast-str (emit-cpp expr-ast-opts)
        return-stmt-str (emit-cpp-statement ["return"
                                              expr-ast-str])]
    return-stmt-str))

;; deref

(defn emit-cpp-deref
  [ast-opts]
  {:pre [(= :invoke (-> ast-opts :ast :op))]}
  (let [ast (:ast ast-opts)
        ;; Note: assuming that there is only one arg to deref, which is the symbol (identifier)
        identifier-symbol (-> ast :args first :form)
        identifier-str (str identifier-symbol)]
    identifier-str))

;; not

(defn emit-cpp-not
  [ast-opts]
  {:pre [(= :invoke (-> ast-opts :ast :op))]}
  (let [ast (:ast ast-opts)
        ;; Note: assuming that not only has 1 arg
        arg-ast (-> ast :args first)
        arg-str (emit-cpp (assoc ast-opts :ast arg-ast))
        expr-str (str "!(" arg-str ")")]
    expr-str))

;; fn invocations

(defn emit-cpp-invoke-arg
  [ast-opts]
  (emit-cpp ast-opts))

(defn emit-cpp-invoke-args
  [ast-opts]
  (let [ast (:ast ast-opts)
        args-ast (:args ast)
        args-ast-opts (map #(assoc ast-opts :ast %) args-ast)
        emitted-args (map emit-cpp-invoke-arg args-ast-opts)]
    emitted-args))

(defn emit-cpp-str-arg
  [ast-opts]
  (let [ast (:ast ast-opts)
        tag-class (:tag ast)
        emitted-arg (emit-cpp ast-opts)
        casted-emitted-arg (if (is-number-type? tag-class)
                             (str "std::to_string(" emitted-arg ")") 
                             emitted-arg)]
    casted-emitted-arg))

(defn emit-cpp-str-args
  [ast-opts]
  (let [ast (:ast ast-opts)
        args-ast (:args ast)
        args-ast-opts (map #(assoc ast-opts :ast %) args-ast)
        emitted-args (map emit-cpp-str-arg args-ast-opts)]
    emitted-args))

(defn emit-cpp-str
  [ast-opts]
  (let [ast (:ast ast-opts)
        arg-strs (emit-cpp-str-args ast-opts)
        expr-parts (interpose " + " arg-strs)
        expr (apply str expr-parts)]
    expr))

(defn emit-cpp-println
  "Emit the equivalent of printing out to std out. To support auto-casting to str, insert a \"\" before the other args"
  [ast-opts]
  (let [ast (:ast ast-opts)
        arg-strs (emit-cpp-invoke-args ast-opts)
        all-arg-strs (concat ["cout"] arg-strs ["endl"])
        command-expr (apply str (interpose " << " all-arg-strs))]
    command-expr))

(defn emit-cpp-invoke
  "handles invocations of known functions"
  [ast-opts]
  {:pre [(= :invoke (:op (:ast ast-opts)))]}
  (let [ast (:ast ast-opts)
        fn-meta-ast (-> ast :fn :meta)]
    (cond
      (fn-matches? fn-meta-ast "clojure.core" "deref")
      (emit-cpp-deref ast-opts)

      (fn-matches? fn-meta-ast "clojure.core" "str")
      (emit-cpp-str ast-opts)

      (fn-matches? fn-meta-ast "clojure.core" "not")
      (emit-cpp-not ast-opts)

      (fn-matches? fn-meta-ast "clojure.core" "println")
      (emit-cpp-println ast-opts)

      (fn-matches? fn-meta-ast "clj-icu-test.core" "defclass")
      (emit-cpp-defclass ast-opts)

      (fn-matches? fn-meta-ast "clj-icu-test.core" "defenum-impl")
      (emit-cpp-defenum ast-opts)

      (fn-matches? fn-meta-ast "clj-icu-test.core" "return")
      (emit-cpp-return ast-opts)
      
      )))

;; loops (ex: while, doseq)

(defn emit-cpp-while
  [ast-opts]
  {:pre [(= :loop (:op (:ast ast-opts)))]}
  (let [ast (:ast ast-opts)
        test-ast (-> ast :body :test)
        test-str (emit-cpp (assoc ast-opts :ast test-ast))
        then-ast (-> ast :body :then)
        ;; Note: we are ignoring the "return" expression stored in (-> ast :body :then :ret)
        ;; when we only look at (-> ast :body :then :statements)
        statements (:statements then-ast)
        body-strs (indent
                   (let [statement-ast-opts (map #(assoc ast-opts :ast %) statements)
                         statement-strs (map emit-cpp statement-ast-opts)]
                     statement-strs))
        body-strs-with-semicolons (indent
                                   (map #(if-not (can-become-cpp-statement %)
                                           %
                                           (emit-cpp-statement [%]))
                                        body-strs))
        body-str (string/join "\n" body-strs-with-semicolons)
        while-parts [(str (indent-str-curr-level) "while (" test-str ")")
                     (str (indent-str-curr-level) "{")
                     body-str
                     (str (indent-str-curr-level) "}")]
        while-str (string/join "\n" while-parts)]
    while-str))

(defn emit-cpp-loop
  [ast-opts]
  {:pre [(= :loop (:op (:ast ast-opts)))]}
  (let [ast (:ast ast-opts)
        form-symbol (-> ast :raw-forms first first)
        form-symbol-str (str form-symbol)]
    (case form-symbol-str
      "while" (emit-cpp-while ast-opts))))

;; entry point

(defn emit-cpp
  [ast-opts]
  (let [ast (:ast ast-opts)]
    ;; TODO: some multimethod ?
    (case (:op ast)
      :def (case (some-> ast :raw-forms last first name)
             "defn" (emit-cpp-defn ast-opts)
             (emit-cpp-def ast-opts))
      :const (emit-cpp-const ast-opts)
      :invoke (case (-> ast :fn :meta :name name)
                "atom" (emit-cpp-atom ast-opts)
                "reset!" (emit-cpp-reset! ast-opts)
                (emit-cpp-invoke ast-opts)) 
      :do (emit-cpp-do ast-opts)
      :let (emit-cpp-let ast-opts)
      :local (emit-cpp-local ast-opts)
      :static-call (emit-cpp-static-call ast-opts)
      :var (emit-cpp-var ast-opts)
      :loop (emit-cpp-loop ast-opts)
      :else (cond 
              (:raw-forms ast)
              (emit-cpp (assoc ast-opts :ast (-> ast :raw-forms)))))))

