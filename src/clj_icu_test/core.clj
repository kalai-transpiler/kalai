(ns clj-icu-test.core
  (:require [clojure.string :as string]
            [clojure.tools.analyzer.jvm :as az]))

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

(defn emit-cpp-def
  [ast]
  {:pre [(= :def (:op ast))]}
  (let [type-class (or (get-in ast [:meta :val :tag])
                       (get-in ast [:init :env :tag]))
        type-str (emit-cpp-type type-class)
        identifier (name (:name ast))
        expression (emit-cpp (:init ast))
        statement-parts [type-str
                         identifier
                         "="
                         expression]
        statement (emit-cpp-statement statement-parts)]
    statement))

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
  (str (->> statement-parts
            (keep identity)
            (map str)
            (string/join " "))
       ";"))

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

(defn emit-java-def
  [ast]
  {:pre [(= :def (:op ast))]}
  (let [type-class (or (get-in ast [:meta :val :tag])
                       (get-in ast [:init :env :tag]))
        type-str (emit-java-type type-class)
        identifier (name (:name ast))
        expression (emit-java (:init ast))
        statement-parts [type-str
                         identifier
                         "="
                         expression]
        statement (emit-java-statement statement-parts)]
    statement))

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
    :do (emit-java-do ast)))
