(ns clj-icu-test.emit.impl.rust
  (:require [clj-icu-test.common :refer :all]
            [clj-icu-test.emit.interface :as iface :refer :all]
            [clj-icu-test.emit.langs :as l]
            [clj-icu-test.emit.impl.util.java-type-util :as java-type-util]
            [clj-icu-test.emit.impl.util.common-type-util :as common-type-util]
            ;;[clojure.edn :as edn]
            [clojure.string :as string]
            [clojure.tools.analyzer.jvm :as az]))

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
                                           java.lang.StringBuffer "String"}]
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
        is-immutable true
        identifier-and-type (if type-str
                              (if is-immutable
                                [(str "let " identifier ":") type-str]
                                [(str "let mut " identifier ":") type-str])
                              [identifier])
        statement-parts (concat
                         identifier-and-type
                         ["="
                          expression])
        statement-parts-opts (-> ast-opts
                                 (assoc :val statement-parts)
                                 map->AnyValOpts)
        statement (emit-statement statement-parts-opts)]
        statement))
