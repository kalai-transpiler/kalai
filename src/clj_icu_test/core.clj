(ns clj-icu-test.core
  (:require [clojure.string :as string]
            [clojure.tools.analyzer.jvm :as az]))

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

(defn emit-java
  [ast]
  ;; TODO: some multimethod ?
  (case (:op ast)
    :def (emit-java-def ast)
    :const (emit-java-const ast))
  )
