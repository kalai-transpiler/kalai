(ns kalai.pass.java-string
  (:require [meander.strategy.epsilon :as s]
            [clojure.string :as str]))

(declare stringify)

;;;; These are helpers

(defn- param-list [params]
  (str "(" (str/join ", " params) ")"))

(defn- space-separated [& xs]
  (str/join " " xs))


;;;; These are what our symbols should resolve to

(defn expression-statement-str [x]
  (str (stringify x) ";"))

(defn block-str [& xs]
  (str "{"
       (str/join \newline (map stringify xs))
       "}"))

(defn assignment-str [variable-name value]
  (str "int " variable-name "=" value))

#_(defn const [bindings]
  (str "const" Type x "=" initialValue))

#_(defn test* [x]
  ;; could be a boolean expression
  (str x "==" y)
  ;; or just a value
  (str x))


#_(defn conditional [test then else] )

(defn invocation-str [function-name args]
  (str function-name (param-list args)))

(defn function-str [return-type name doc params body]
  (space-separated 'public 'static
                   return-type
                   name
                   (param-list (for [param params]
                                 (str (or (some-> param meta :tag)
                                          "var")
                                      " "
                                      param)))
                   (stringify body)))

(defn operator-str [op x y]
  (str x op y))

(defn class-str [class-name body]
  (space-separated 'public 'class class-name
                   (stringify body)))

(defn return-str [x]
  (space-separated 'return (stringify x)))

;;;; This is the main entry point

(def str-fn-map
  {'j/class class-str
   'j/operator operator-str
   'j/function function-str
   'j/invocation invocation-str
   'j/assignment assignment-str
   'j/block block-str
   'j/expression-statement expression-statement-str
   'j/return return-str})

(def stringify
  (s/match
    (?x . !more ...)
    (let [f (get str-fn-map ?x)]
      (apply f !more))

    ?else (str ?else)))
