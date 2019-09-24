(ns clj-icu-test.emit.impl.java
  (:require [clj-icu-test.common :refer :all]
            [clj-icu-test.emit.interface :as iface :refer :all]
            [clj-icu-test.emit.langs :as l]
            [clj-icu-test.emit.impl.util.java-type-util :as type-util]
            [clojure.edn :as edn]
            [clojure.string :as string]
            [clojure.tools.analyzer.jvm :as az]))

(defmethod iface/emit-complex-type ::l/java
  [ast-opts]
  (let [ast (:ast ast-opts)
        type-val (:mtype ast)]
    ;; support list types only, for now
    (assert (= java.util.List (first type-val)))
    (let [type-parameter-val (second type-val)
          _ (assert (sequential? type-parameter-val))
          type-parameter-class-ast-opts (assoc-in ast-opts [:ast :mtype] type-parameter-val)
          type-parameter (emit-type type-parameter-class-ast-opts)
          type (str "List<" type-parameter ">")]
      type)))

(defmethod iface/emit-scalar-type ::l/java
  [ast-opts]
  (let [ast (:ast ast-opts)
        class (or (:return-tag ast)
                  (:tag ast)
                  (:mtype ast))]
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

(defmethod iface/emit-statement ::l/java
  [val-opts]
  {:pre [(= clj_icu_test.common.AnyValOpts (class val-opts))]}
  (let [statement-parts (:val val-opts)]
    (if (string? statement-parts)
      (let [statement statement-parts]
        (if (= \; (last statement))
          statement
          (str (indent-str-curr-level)
               statement
               ";")))
      (str (indent-str-curr-level)
           (->> statement-parts
                (keep identity)
                (map str)
                (string/join " "))
           ";"))))

(defmethod iface/can-become-statement ::l/java
  [val-opts]
  {:pre [(= clj_icu_test.common.AnyValOpts (class val-opts))]}
  (let [expression (:val val-opts)]
    (let [result
          (let [last-char (last expression)]
            (and (not= \; last-char)
                 (not= \} last-char)))]
      result)))

(defmethod iface/emit-const-complex-type [::l/java :vector]
  [ast-opts]
  (type-util/java-emit-const-complex-type ast-opts))

(defmethod iface/emit-assignment-complex-type [::l/java :vector]
  [ast-opts]
  {:pre [(or (and (= :const (-> ast-opts :ast :init :op))
                  (= :vector (-> ast-opts :ast :init :type)))
             (= :vector (-> ast-opts :ast :init :op)))]}
  (let [ast (:ast ast-opts)
        type-class-ast (get-assignment-type-class-ast ast-opts)
        type-class-ast-opts (assoc ast-opts :ast type-class-ast)
        type-str (emit-type type-class-ast-opts) 
        identifier (when-let [identifer-symbol (get-assignment-identifier-symbol ast-opts)]
                     (str identifer-symbol)) 
        expr-ast-opts (update-in ast-opts [:ast] :init)
        expr (type-util/java-emit-const-complex-type expr-ast-opts) 
        statement-parts [type-str
                         identifier
                         "="
                         expr]
        statement-parts-opts (-> ast-opts
                                 (assoc :val statement-parts)
                                 map->AnyValOpts)
        statement (emit-statement statement-parts-opts)]
    statement))

(defmethod iface/emit-defn ::l/java 
  [ast-opts]
  {:pre [(= :def (-> ast-opts :ast :op))]}
  (let [ast (:ast ast-opts)
        fn-name (:name ast)
        fn-ast (:init ast)
        fn-ast-opts (assoc ast-opts :ast fn-ast)
        fn-return-type (emit-type fn-ast-opts) 
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

;; classes (or modules or namespaces)

(defmethod iface/emit-defclass ::l/java
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
                                         (map #(if-not (can-become-statement %)
                                                 (:val %)
                                                 (emit-statement %))
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

(defmethod iface/emit-defenum ::l/java
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

(defmethod iface/emit-str ::l/java
  [ast-opts]
  (let [ast (:ast ast-opts)
        arg-strs (emit-invoke-args ast-opts)
        arg-append-strs (map #(str ".append(" % ")") arg-strs)
        expr-parts (concat ["new StringBuffer()"]
                           arg-append-strs
                           [".toString()"])
        expr (apply str expr-parts)]
    expr))

(defmethod iface/emit-println ::l/java
  [ast-opts]
  (let [ast (:ast ast-opts)
        arg-strs (emit-invoke-args ast-opts)
        all-arg-strs (cons "\"\"" arg-strs)
        command-expr (str "System.out.println("
                          (apply str (interpose " + " all-arg-strs))
                          ")")]
    command-expr))

(defmethod iface/emit-new-strbuf ::l/java
  [ast-opts]
  ;; Note: currently assuming that there are 0 args to StringBuffer,
  ;; but can support args later
  "new StringBuffer()")

(defmethod iface/emit-prepend-strbuf ::l/java
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

(defmethod iface/emit-tostring-strbuf ::l/java
  [ast-opts]
  (let [ast (:ast ast-opts)
        args (:args ast)
        arg-strs (emit-invoke-args ast-opts)
        obj-name (first arg-strs)
        tostring-invoke (str obj-name ".toString()")]
    tostring-invoke))

(defmethod iface/emit-invoke ::l/java
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

;; new

(defmethod iface/emit-new ::l/java
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
