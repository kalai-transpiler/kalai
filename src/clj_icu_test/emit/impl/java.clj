(ns clj-icu-test.emit.impl.java
  (:require [clj-icu-test.common :refer :all]
            [clj-icu-test.emit.interface :as iface :refer :all]
            [clj-icu-test.emit.langs :as l]
            [clojure.edn :as edn]
            [clojure.string :as string]
            [clojure.tools.analyzer.jvm :as az]))
;;
;; stuff that needs to be in defmethods
;;

(defn emit-java-type
  "Might return nil"
  [val-opts]
  {:pre [(= clj_icu_test.common.AnyValOpts (class val-opts))]}
  (let [class (:val val-opts)]
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
                  nil))))))

(defn emit-java-statement
  "input is a seq of strings"
  [val-opts]
  {:pre [(= clj_icu_test.common.AnyValOpts (class val-opts))]}
  (let [statement-parts (:val val-opts)]
    (if (string? statement-parts)
      (let [statement statement-parts]
        (str (indent-str-curr-level)
             statement
             ";"))
      (str (indent-str-curr-level)
           (->> statement-parts
                (keep identity)
                (map str)
                (string/join " "))
           ";"))))

(defn can-become-java-statement
  "input is a string representing a statement"
  [val-opts]
  {:pre [(= clj_icu_test.common.AnyValOpts (class val-opts))]}
  (let [expression (:val val-opts)]
    (let [result
          (let [last-char (last expression)]
            (and (not= \; last-char)
                 (not= \} last-char)))]
      result)))

;;
;; defmethods
;;

(defmethod iface/emit-const :l/java
  [ast-opts]
 {:pre [(= :const (:op (:ast ast-opts)))
         (:literal? (:ast ast-opts))]}
  (let [ast (:ast ast-opts)]
    (pr-str (:val ast))))

(defmethod iface/emit-do :l/java
  [ast-opts]
  {:pre [(= :do (:op (:ast ast-opts)))]}
  (let [ast (:ast ast-opts)
        stmts (:statements ast)
        stmts-ast-opts (map (partial assoc ast-opts :ast) stmts)
        stmt-emitted-lines (map emit stmts-ast-opts)
        last-stmt (:ret ast)
        last-emitted-line (emit (assoc ast-opts :ast last-stmt))
        all-lines (concat stmt-emitted-lines [last-emitted-line])]
    all-lines))

;; bindings

(defmethod iface/emit-atom :l/java
  [ast-opts]
  {:pre [(and (= :invoke (:op (:ast ast-opts)))
              (= (symbol "atom") (-> ast-opts :ast :fn :meta :name)))]}
  (let [ast (:ast ast-opts)
        init-val-ast (-> ast
                         :args
                         first)]
    (emit (assoc ast-opts :ast init-val-ast))))

(defmethod iface/emit-reset! :l/java
  [ast-opts]
  {:pre [(and (= :invoke (:op (:ast ast-opts)))
              (= (symbol "reset!") (-> ast-opts :ast :fn :meta :name)))]}
  (let [ast (:ast ast-opts)
        identifier (str (or (-> ast :args first :meta :name)
                            (-> ast :args first :form)))
        reset-val-ast (-> ast
                          :args
                          second)
        expression (emit (assoc ast-opts :ast reset-val-ast))
        statement-parts [identifier
                         "="
                         expression]
        statement-parts-opts (-> ast-opts
                                 (assoc :val statement-parts)
                                 map->AnyValOpts)
        statement (emit-java-statement statement-parts-opts)]
    statement))

(defmethod iface/emit-assignment :l/java
  [ast-opts]
  (let [ast (:ast ast-opts)
        op-code (:op ast) 
        type-class (or (get-in ast [:meta :val :tag])
                       (get-in ast [:init :env :tag])
                       (and (= :binding op-code)
                            (get ast :tag)))
        type-class-opts (-> ast-opts
                            (assoc :val type-class)
                            map->AnyValOpts)
        type-str (emit-java-type type-class-opts)
        identifier (when-let [identifer-symbol (or (get-in ast [:env :form])
                                                   (case op-code
                                                     :binding (get ast :form)
                                                     :def (get ast :name)))]
                     (str identifer-symbol))
        expression (emit (assoc ast-opts :ast (:init ast)))
        statement-parts [type-str
                         identifier
                         "="
                         expression]
        statement-parts-opts (-> ast-opts
                                 (assoc :val statement-parts)
                                 map->AnyValOpts)
        statement (emit-java-statement statement-parts-opts)]
    statement))

(defmethod iface/emit-def :l/java
  [ast-opts]
  {:pre [(= :def (:op (:ast ast-opts)))]}
  (let [ast (:ast ast-opts)]
    (emit-assignment ast-opts)))

(defmethod iface/emit-binding :l/java
  [ast-opts]
  {:pre [(= :binding (:op (:ast ast-opts)))]}
  (let [ast (:ast ast-opts)]
    (emit-assignment ast-opts)))

(defmethod iface/emit-bindings-stanza :l/java
  [ast-opts]
  {:pre [(sequential? (:ast ast-opts))]}
  (let [ast (:ast ast-opts)
        bindings ast ;; the AST in this case is a vector of sub-ASTs
        bindings-ast-opts (map (partial assoc ast-opts :ast) bindings)
        binding-statements (map emit-binding bindings-ast-opts)]
    (when-let [non-nil-binding-statements (->> binding-statements
                                               (keep identity)
                                               seq)]
      (let [bindings-str (string/join "\n" non-nil-binding-statements)] 
        bindings-str))))

(defmethod iface/emit-let :l/java
  [ast-opts]
  {:pre [(= :let (:op (:ast ast-opts)))]}
  (let [ast (:ast ast-opts)
        bindings (:bindings ast)
        binding-str (indent (emit-bindings-stanza (assoc ast-opts :ast bindings)))
        body-ast (:body ast)
        body-strs (indent
                   (if (:statements body-ast)
                     ;; if ast has key nesting of [:body :statements], then we have a multi-"statement" expression do block in the let form
                     (let [butlast-statements (:statements body-ast)
                           last-statement (:ret body-ast)
                           statements (concat butlast-statements [last-statement])
                           statement-ast-opts (map #(assoc ast-opts :ast %) statements)
                           statement-strs (map emit statement-ast-opts)]
                       statement-strs)
                     ;; else the let block has only one "statement" in the do block
                     [(emit (assoc ast-opts :ast body-ast))]))
        body-strs-opts-seq (map #(-> ast-opts
                                     (assoc :val %)
                                     map->AnyValOpts)
                                body-strs)
        body-strs-with-semicolons (indent
                                   (map #(if-not (can-become-java-statement %)
                                           (:val %)
                                           (emit-java-statement %))
                                        body-strs-opts-seq))
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

(defmethod iface/emit-arg :l/java
  [ast-opts symb]
  {:pre [(:env ast-opts)]}
  (let [ast-opts-env (:env ast-opts)
        symb-class (class symb)
        symb-ast (az/analyze symb ast-opts-env)
        symb-ast-opts (assoc ast-opts :ast symb-ast)]
    (cond

      ;; emit a "standalone" token
      (= clojure.lang.Symbol symb-class)
      (emit symb-ast-opts)
      
      ;; a seq of symbols -> emit parenthases around the emitted form
      (or (isa? symb-class clojure.lang.IPersistentCollection)
          (isa? symb-class clojure.lang.ISeq))
      (if (= 1 (count symb-ast))
        (emit (assoc symb-ast-opts :ast (first symb-ast)))
        (str "(" (emit symb-ast-opts) ")"))

      ;; else, we have something that we treat like a scalar
      :else (emit symb-ast-opts))))

(defmethod iface/emit-args :l/java
  [ast-opts]
  {:pre [(-> ast-opts :ast :raw-forms seq)]}
  (let [ast (:ast ast-opts)
        raw-forms (-> ast :raw-forms)
        raw-form-arg-symbols (-> raw-forms
                                 last
                                 rest)
        raw-form-arg-symbol-ast-opts (assoc ast-opts :env (-> ast :env))
        emitted-args (map (partial emit-arg raw-form-arg-symbol-ast-opts) raw-form-arg-symbols)]
    emitted-args))

(defmethod iface/emit-static-call :l/java
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
        arg-strs (emit-args ast-opts)
        expression-parts (interpose static-call-fn-symbol arg-strs)
        expression (string/join " " expression-parts)]
    expression))


;; other

(defmethod iface/emit-local :l/java
  [ast-opts]
  {:pre [(= :local (:op (:ast ast-opts)))]}
  (let [ast (:ast ast-opts)
        form (:form ast)]
    (str (name form))))

(defmethod iface/emit-var :l/java
  [ast-opts]
  {:pre [(= :var (:op (:ast ast-opts)))]}
  (let [ast (:ast ast-opts)
        form (:form ast)]
    (str (name form))))

;; functions

(defmethod iface/emit-defn-arg :l/java
  [ast-opts]
  {:pre [(= :binding (-> ast-opts :ast :op))]}
  (let [ast (:ast ast-opts)
        arg-name (-> ast :form name)
        type-class (-> ast :tag)
        type-class-opts (-> ast-opts
                            (assoc :val type-class)
                            map->AnyValOpts)
        type-str (emit-java-type type-class-opts)
        identifier-signature-parts [type-str
                                    arg-name]
        identifier-signature (->> identifier-signature-parts
                                  (keep identity)
                                  (string/join " "))]
    identifier-signature))

(defmethod iface/emit-defn-args :l/java
  [ast-opts]
  ;; Note: can have empty args
  ;;{:pre [(seq (:ast ast-opts))]}
  (let [ast (:ast ast-opts)
        arg-ast-seq ast
        arg-ast-opts (->> arg-ast-seq
                          (map #(assoc ast-opts :ast %))
                          (map emit-defn-arg))]
    (string/join ", " arg-ast-opts)))

(defmethod iface/emit-defn :l/java 
  [ast-opts]
  {:pre [(= :def (-> ast-opts :ast :op))]}
  (let [ast (:ast ast-opts)
        fn-name (:name ast)
        fn-ast (:init ast)
        fn-return-type-class (:return-tag fn-ast)
        fn-return-type-class-opts (-> ast-opts
                                      (assoc :val fn-return-type-class)
                                      map->AnyValOpts)
        fn-return-type (emit-java-type fn-return-type-class-opts)
        ;; Note: currently not dealing with fn overloading (variadic fns in Clojure),
        ;; so just take the first fn method
        fn-method-first (-> fn-ast
                            :expr
                            :methods first)
        fn-method-first-arg-asts (:params fn-method-first)
        fn-method-first-args (-> (assoc ast-opts :ast fn-method-first-arg-asts)
                                 emit-defn-args)
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
                                           statement-strs (map emit statement-ast-opts)]
                                       statement-strs)
                                     ;; else the let block has only one "statement" in the do block
                                     [(emit (assoc ast-opts :ast fn-method-first-body-ast))]))
        fn-method-first-body-strs-opts-seq (map #(-> ast-opts
                                                     (assoc :val %)
                                                     map->AnyValOpts)
                                                fn-method-first-body-strs)
        fn-method-first-body-strs-with-semicolons (indent
                                                   (map #(if-not (can-become-java-statement %)
                                                           (:val %)
                                                           (emit-java-statement %))
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

;; classes (or modules or namespaces)

(defmethod iface/emit-defclass :l/java
  [ast-opts]
  {:pre [(= :invoke (-> ast-opts :ast :op))]}
  (let [ast (:ast ast-opts)
        ;; Currently assuming that the class-name is provided as String
        class-name (-> ast :args first :val)
        ;; Note: making all classes public b/c no reason to do otherwise currently,
        ;; see emit-defn for reasoning.
        class-signature-parts ["public"
                               "class"
                               class-name]
        class-signature (string/join " " class-signature-parts)
        class-form-asts (-> ast :args rest)
        class-form-ast-opts (map (partial assoc ast-opts :ast) class-form-asts)
        class-form-strs (indent
                         (map emit class-form-ast-opts))
        class-form-strs-opts-seq (map #(-> ast-opts
                                          (assoc :val %)
                                          map->AnyValOpts)
                                      class-form-strs)
        class-form-strs-with-semicolons (indent
                                         (map #(if-not (can-become-java-statement %)
                                                 (:val %)
                                                 (emit-java-statement %))
                                              class-form-strs-opts-seq))
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

(defmethod iface/emit-defenum :l/java
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
        enum-field-strs (map emit enum-field-ast-opts)
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

(defmethod iface/emit-return :l/java
  [ast-opts]
  {:pre [(= :invoke (-> ast-opts :ast :op))]}
  (let [ast (:ast ast-opts)
        expr-ast (-> ast :args first)
        expr-ast-opts (assoc ast-opts :ast expr-ast)
        expr-ast-str (emit expr-ast-opts)
        expr-ast-str-opts (-> ast-opts
                              (assoc :val ["return"
                                           expr-ast-str])
                              map->AnyValOpts)
        return-stmt-str (emit-java-statement expr-ast-str-opts)]
    return-stmt-str))

;; deref

(defmethod iface/emit-deref :l/java
  [ast-opts]
  {:pre [(= :invoke (-> ast-opts :ast :op))]}
  (let [ast (:ast ast-opts)
        ;; Note: assuming that there is only one arg to deref, which is the symbol (identifier)
        identifier-symbol (-> ast :args first :form)
        identifier-str (str identifier-symbol)]
    identifier-str))

;; not

(defmethod iface/emit-not :l/java
  [ast-opts]
  {:pre [(= :invoke (-> ast-opts :ast :op))]}
  (let [ast (:ast ast-opts)
        ;; Note: assuming that not only has 1 arg
        arg-ast (-> ast :args first)
        arg-str (emit (assoc ast-opts :ast arg-ast))
        expr-str (str "!(" arg-str ")")]
    expr-str))

;; fn invocations

(defmethod iface/emit-invoke-arg :l/java
  [ast-opts]
  (emit ast-opts))

(defmethod iface/emit-invoke-args :l/java
  [ast-opts]
  (let [ast (:ast ast-opts)
        args-ast (:args ast)
        args-ast-opts (map #(assoc ast-opts :ast %) args-ast)
        emitted-args (map emit-invoke-arg args-ast-opts)]
    emitted-args))

(defmethod iface/emit-str :l/java
  [ast-opts]
  (let [ast (:ast ast-opts)
        arg-strs (emit-invoke-args ast-opts)
        arg-append-strs (map #(str ".append(" % ")") arg-strs)
        expr-parts (concat ["new StringBuffer()"]
                           arg-append-strs
                           [".toString()"])
        expr (apply str expr-parts)]
    expr))

(defmethod iface/emit-println :l/java
  [ast-opts]
  (let [ast (:ast ast-opts)
        arg-strs (emit-invoke-args ast-opts)
        all-arg-strs (cons "\"\"" arg-strs)
        command-expr (str "System.out.println("
                          (apply str (interpose " + " all-arg-strs))
                          ")")]
    command-expr))

(defmethod iface/emit-new-strbuf :l/java
  [ast-opts]
  ;; Note: currently assuming that there are 0 args to StringBuffer,
  ;; but can support args later
  "new StringBuffer()")

(defmethod iface/emit-prepend-strbuf :l/java
  [ast-opts]
  (let [ast (:ast ast-opts)
        args (:args ast)
        arg-strs (emit-invoke-args ast-opts)
        obj-name (first arg-strs)
        prepended-val-str (second arg-strs)
        prepend-invoke-parts [obj-name
                              ".insert("
                              0
                              ", "
                              prepended-val-str
                              ")"]
        prepend-invoke (apply str prepend-invoke-parts)]
    prepend-invoke))

(defmethod iface/emit-tostring-strbuf :l/java
  [ast-opts]
  (let [ast (:ast ast-opts)
        args (:args ast)
        arg-strs (emit-invoke-args ast-opts)
        obj-name (first arg-strs)
        tostring-invoke (str obj-name ".toString()")]
    tostring-invoke))

(defmethod iface/emit-invoke :l/java
  [ast-opts]
  {:pre [(= :invoke (:op (:ast ast-opts)))]}
  (let [ast (:ast ast-opts)
        fn-meta-ast (-> ast :fn :meta)]
    (cond
      (fn-matches? fn-meta-ast "clojure.core" "deref")
      (emit-deref ast-opts)

      (fn-matches? fn-meta-ast "clojure.core" "str")
      (emit-str ast-opts)

      (fn-matches? fn-meta-ast "clojure.core" "not")
      (emit-not ast-opts)

      (fn-matches? fn-meta-ast "clojure.core" "println")
      (emit-println ast-opts)

      (fn-matches? fn-meta-ast "clj-icu-test.common" "defclass")
      (emit-defclass ast-opts)

      (fn-matches? fn-meta-ast "clj-icu-test.common" "defenum-impl")
      (emit-defenum ast-opts)

      (fn-matches? fn-meta-ast "clj-icu-test.common" "return")
      (emit-return ast-opts)

      (fn-matches? fn-meta-ast "clj-icu-test.common" "new-strbuf")
      (emit-new-strbuf ast-opts)

      (fn-matches? fn-meta-ast "clj-icu-test.common" "prepend-strbuf")
      (emit-prepend-strbuf ast-opts)

      (fn-matches? fn-meta-ast "clj-icu-test.common" "tostring-strbuf")
      (emit-tostring-strbuf ast-opts)
      
      :else
      (let [fn-ns (-> fn-meta-ast :ns str)
            fn-name (-> fn-meta-ast :name)]
        (throw (Exception. (str "Function call not recognized for "
                                fn-ns "/" fn-name)))))))

;; loops (ex: while, doseq)

(defmethod iface/emit-while :l/java
  [ast-opts]
  {:pre [(= :loop (:op (:ast ast-opts)))]}
  (let [ast (:ast ast-opts)
        test-ast (-> ast :body :test)
        test-str (emit (assoc ast-opts :ast test-ast))
        then-ast (-> ast :body :then)
        ;; Note: we are ignoring the "return" expression stored in (-> ast :body :then :ret)
        ;; when we only look at (-> ast :body :then :statements)
        statements (:statements then-ast)
        body-strs (indent
                   (let [statement-ast-opts (map #(assoc ast-opts :ast %) statements)
                         statement-strs (map emit statement-ast-opts)]
                     statement-strs))
        body-strs-opts-seq (map #(-> ast-opts
                                     (assoc :val %)
                                     map->AnyValOpts)
                                body-strs)
        body-strs-with-semicolons (indent
                                   (map #(if-not (can-become-java-statement %)
                                           (:val %)
                                           (emit-java-statement %))
                                        body-strs-opts-seq))
        body-str (string/join "\n" body-strs-with-semicolons)
        while-parts [(str (indent-str-curr-level) "while (" test-str ")")
                     (str (indent-str-curr-level) "{")
                     body-str
                     (str (indent-str-curr-level) "}")]
        while-str (string/join "\n" while-parts)]
    while-str))

(defmethod iface/emit-loop :l/java
  [ast-opts]
  {:pre [(= :loop (:op (:ast ast-opts)))]}
  (let [ast (:ast ast-opts)
        form-symbol (-> ast :raw-forms first first)
        form-symbol-str (str form-symbol)]
    (case form-symbol-str
      "while" (emit-while ast-opts))))

;; new

(defmethod iface/emit-new :l/java
  [ast-opts]
  {:pre [(= :new (:op (:ast ast-opts)))]}
  (let [ast (:ast ast-opts)
        new-class-name (-> ast :class :form)
        ;; reuse invoke-args helper fns here
        arg-strs (emit-invoke-args ast-opts)
        arg-str (string/join ", " arg-strs)
        new-str-parts ["new"
                       (apply str [new-class-name
                                   "("
                                   arg-str
                                   ")"])]
        new-str (string/join " " new-str-parts)]
    new-str))

;; entry point

(defmethod iface/emit :l/java
  [ast-opts]
  (let [ast (:ast ast-opts)]
    ;; TODO: some multimethod ?
    (case (:op ast)
      :def (case (some-> ast :raw-forms last first name)
             "defn" (emit-defn ast-opts)
             (emit-def ast-opts))
      :const (emit-const ast-opts)
      :invoke (case (-> ast :fn :meta :name name)
                "atom" (emit-atom ast-opts)
                "reset!" (emit-reset! ast-opts)
                (emit-invoke ast-opts)) 
      :do (emit-do ast-opts)
      :let (emit-let ast-opts)
      :local (emit-local ast-opts)
      :static-call (emit-static-call ast-opts)
      :var (emit-var ast-opts)
      :loop (emit-loop ast-opts)
      :new (emit-new ast-opts)
      :else (cond 
              (:raw-forms ast)
              (emit (assoc ast-opts :ast (-> ast :raw-forms)))))))
