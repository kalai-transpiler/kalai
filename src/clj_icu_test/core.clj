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
                             java.lang.Long "long int"
                             java.lang.Float "float"
                             java.lang.Double "double float"
                             java.lang.Boolean "bool"
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
    (let [canonical-name (.getCanonicalName class)]
      (if (.startsWith canonical-name "java.lang.")
        (subs canonical-name 10)
        canonical-name))))

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

;; bindings

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
    :const (emit-java-const ast))
  )
