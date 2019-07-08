(ns clj-icu-test.core
  (:require [clojure.string :as string]
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
    (str (:val ast))))

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
      ;; implicit type signatures applied for bindings in a let block
      ;; (= long class) "long"
      ;; (= int class) "int"
      ;; (= char class) "char"
      ;; (= boolean class) "boolean"
      :else (let [canonical-name (.getCanonicalName class)] 
              (cond                
                ;; this is to prevent the analyzer from auto-tagging the type classes of
                ;; symbols in a binding form in a way that is currently being assumed to
                ;; be unwanted in the emitted output.
                ;; If we need to actually emit a type signature of Object in the future,
                ;; we can subclass Object to a custom type (ex: ExplicitlyAnObject.java)
                ;; and tell users to use that new class in their type hints if they want
                ;; a type signature of java.lang.Object in emitted Java output.
                (= "java.lang.Object" canonical-name)
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
  [statement-parts]
  (str (indent-str-curr-level)
       (->> statement-parts
            (keep identity)
            (map str)
            (string/join " "))
       ";"))

(defn can-become-java-statement
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
    (str (:val ast))))

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
                     [(emit-java (assoc ast-opts :ast (:body ast)))]))
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

;; (defn flatten-static-call-args
;;   "Convert the :args field of the ast into a seq of subtrees of operands, but also flatten any nesting of :args-keyed operands into a flat seq.
;;   Recursive function.
;;   May return nil"
;;   [ast]
;;   (when ast
;;     (if-not (sequential? (:args ast))
;;       [(:args ast)]
;;       (->> (mapcat
;;             (fn [arg-ast] (if (:args arg-ast)
;;                             (flatten-static-call-args arg-ast)
;;                             [arg-ast]))
;;              (:args ast))
;;            (keep identity)))))

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
        static-call-fn-symbol (-> ast :raw-forms last first)
        ;; flattened-args (or (when (-> ast :body :args)
        ;;                      (flatten-static-call-args (-> ast :body)))
        ;;                    (when (-> ast :args)
        ;;                      (flatten-static-call-args ast)))
        ;; arg-strs (map emit-java flattened-args)
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

;; entry point

(defn emit-java
  [ast-opts]
  (let [ast (:ast ast-opts)]
    ;; TODO: some multimethod ?
    (case (:op ast)
      :def (emit-java-def ast-opts)
      :const (emit-java-const ast-opts)
      :invoke (case (-> ast :fn :meta :name name)
                "atom" (emit-java-atom ast-opts)
                "reset!" (emit-java-reset! ast-opts))
      :do (emit-java-do ast-opts)
      :let (emit-java-let ast-opts)
      :local (emit-java-local ast-opts)
      :static-call (emit-java-static-call ast-opts)
      :var (emit-java-var ast-opts)
      :else (cond 
              (:raw-forms ast)
              (emit-java (assoc ast-opts :ast (-> ast :raw-forms)))))))
