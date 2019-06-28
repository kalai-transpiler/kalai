(ns clj-icu-test.core
  (:require [clojure.string :as string]
            [clojure.tools.analyzer.jvm :as az]))

;;
;; constants
;;

(def INDENT-CHAR \space)

(def INDENT-LEVEL-INCREMENT 2)

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
  [ast]
  {:pre [(= :const (:op ast))
         (:literal? ast)]}
  (str (:val ast)))

;; bindings


(defn emit-cpp-assignment
  "To be used by both 'def' and any bindings block of a form (ex: let)
  Might return nil"
  [ast]
  (let [op-code (:op ast)
        type-class (or (get-in ast [:meta :val :tag])
                       (get-in ast [:init :env :tag]))
        type-str (emit-cpp-type type-class)
        identifier (when-let [identifer-symbol (or (get-in ast [:env :form])
                                                   (case op-code
                                                     :binding (get ast :form)
                                                     :def (get ast :name)))]
                     (str identifer-symbol))
        expression (emit-cpp (:init ast))
        statement-parts [type-str
                         identifier
                         "="
                         expression]
        statement (emit-cpp-statement statement-parts)]
    statement))

(defn emit-cpp-def
  [ast]
  {:pre [(= :def (:op ast))]}
  (emit-cpp-assignment ast))

;; entry point

(defn emit-cpp
  [ast]
  ;; TODO: some multimethod ?
  (case (:op ast)
    :def (emit-cpp-def ast)
    :const (emit-cpp-const ast))
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
      (= long class) "long"
      (= int class) "int"
      (= char class) "char"
      (= boolean class) "boolean"
      :else (let [canonical-name (.getCanonicalName class)]
              (if (.startsWith canonical-name "java.lang.")
                (subs canonical-name 10)
                canonical-name)))))

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
  [ast]
  {:pre [(= :const (:op ast))
         (:literal? ast)]}
  (str (:val ast)))

(defn emit-java-do
  [ast]
  {:pre [(= :do (:op ast))]}
  (let [stmts (:statements ast)
        stmt-emitted-lines (map emit-java stmts)
        last-stmt (:ret ast)
        last-emitted-line (emit-java last-stmt)
        all-lines (concat stmt-emitted-lines [last-emitted-line])]
    all-lines))

;; bindings

(defn emit-java-atom
  [ast]
  {:pre [(and (= :invoke (:op ast))
              (= (symbol "atom") (-> ast :fn :meta :name)))]}
  (let [init-val-ast (-> ast
                         :args
                         first)]
    (emit-java init-val-ast)))

(defn emit-java-reset!
  [ast]
  {:pre [(and (= :invoke (:op ast))
              (= (symbol "reset!") (-> ast :fn :meta :name)))]}
  (let [identifier (-> ast :args first :meta :name name)
        reset-val-ast (-> ast
                          :args
                          second)
        expression (-> reset-val-ast emit-java)
        statement-parts [identifier
                         "="
                         expression]
        statement (emit-java-statement statement-parts)]
    statement))

(defn emit-java-assignment
  "To be used by both 'def' and any bindings block of a form (ex: let)
  Might return nil"
  [ast]
  (let [op-code (:op ast)
        type-class (or (get-in ast [:meta :val :tag])
                       (get-in ast [:init :env :tag]))
        type-str (emit-java-type type-class)
        identifier (when-let [identifer-symbol (or (get-in ast [:env :form])
                                                   (case op-code
                                                     :binding (get ast :form)
                                                     :def (get ast :name)))]
                     (str identifer-symbol))
        expression (emit-java (:init ast))
        statement-parts [type-str
                         identifier
                         "="
                         expression]
        statement (emit-java-statement statement-parts)]
    statement))

(defn emit-java-def
  [ast]
  {:pre [(= :def (:op ast))]}
  (emit-java-assignment ast))

(defn emit-java-binding
  "Might return nil"
  [ast]
  {:pre [(= :binding (:op ast))]}
  (emit-java-assignment ast))

(defn emit-java-bindings-stanza
  "The bindings stanza in a form (ex: in a let form).
  Might return nil"
  [ast]
  {:pre [(sequential? ast)]}
  (let [bindings ast ;; the AST in this case is a vector of sub-ASTs
        binding-statements (map emit-java-binding bindings)]
    (when-let [non-nil-binding-statements (->> binding-statements
                                               (keep identity)
                                               seq)]
      (let [bindings-str (string/join "\n" non-nil-binding-statements)] 
        bindings-str))))

(defn emit-java-let
  [ast]
  {:pre [(= :let (:op ast))]}
  (let [bindings (:bindings ast)
        binding-str (indent (emit-java-bindings-stanza bindings))
        body-ast (:body ast)
        body-strs (indent
                   (if (:statements body-ast)
                     ;; if ast has key nesting of [:body :statements], then we have a multi-"statement" expression do block in the let form
                     (let [butlast-statements (:statements body-ast)
                           last-statement (:ret body-ast)
                           statements (concat butlast-statements [last-statement])
                           statement-strs (map emit-java statements)]
                       statement-strs)
                     ;; else the let block has only one "statement" in the do block
                     [(emit-java (:body ast))]))
        body-strs-with-semicolons (indent
                                   (map #(if-not (can-become-java-statement %) % (emit-java-statement [%])) body-strs))
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

(defn emit-java-static-call
  [ast]
  {:pre [(= :static-call (:op ast))]}
  (let [static-call-fn-symbol (-> ast :raw-forms last first)
        args (-> ast :raw-forms last rest)
        arg-strs (->> args
                      (map az/analyze)
                      (map emit-java))
        expression-parts (interpose static-call-fn-symbol arg-strs)
        expression (string/join " " expression-parts)]
    expression))


;; other

(defn emit-java-local
  [ast]
  {:pre [(= :local (:op ast))]}
  (let [form (:form ast)]
    (str (name form))))

(defn emit-java-var
  [ast]
  {:pre [(= :var (:op ast))]}
  (let [form (:form ast)]
    (str (name form))))

;; entry point

(defn emit-java
  [ast]
  ;; TODO: some multimethod ?
  (case (:op ast)
    :def (emit-java-def ast)
    :const (emit-java-const ast)
    :invoke (case (-> ast :fn :meta :name name)
              "atom" (emit-java-atom ast)
              "reset!" (emit-java-reset! ast))
    :do (emit-java-do ast)
    :let (emit-java-let ast)
    :local (emit-java-local ast)
    :static-call (emit-java-static-call ast)
    :var (emit-java-var ast)
    :else (cond
            (:raw-forms ast) (emit-java (-> ast
                                            :raw-forms
                                            )))))
