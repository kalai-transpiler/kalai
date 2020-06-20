(ns kalai.pass.java-string
  (:require [meander.strategy.epsilon :as s]
            [clojure.string :as str]))

;;;; This is the main entry point

(def stringify
  (s/match
    (?x . !more ...)
    (let [f (resolve ?x)]
      (apply f !more))

    ?else (str ?else)))


;;;; These are helpers

(defn- param-list [params]
  (str "(" (str/join ", " params) ")"))

(defn- space-separated [& xs]
  (str/join " " xs))


;;;; These are what our symbols should resolve to

(defn expression-statement [x]
  (str (stringify x) ";"))

(defn block [xs]
  (str "{" xs "}"))

(defn assignment [variable-name value]
  (str "int " variable-name "=" value))

#_(defn const [bindings]
  (str "const" Type x "=" initialValue))

#_(defn test* [x]
  ;; could be a boolean expression
  (str x "==" y)
  ;; or just a value
  (str x))


#_(defn conditional [test then else] )

(defn invocation [function-name args]
  (str function-name (param-list args)))


(defn function [return-type name doc params body]
  (space-separated 'public 'static
                   return-type
                   name
                   (param-list params)
                   (stringify body)))
