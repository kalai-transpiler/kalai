(ns kalai.pass.d4-java-string
  (:require [meander.strategy.epsilon :as s]
            [clojure.string :as str]
            [clojure.pprint :as pprint]))

(declare stringify)

;;;; These are helpers

(defn- parens [x]
  (str "(" x ")"))

(defn- param-list [params]
  (parens (str/join ", " params)))

(defn- space-separated [& xs]
  (str/join " " xs))

(defn- line-separated [& xs]
  (str/join \newline xs))


;;;; These are what our symbols should resolve to

(defn expression-statement-str [x]
  (str (stringify x) ";"))

(defn block-str [& xs]
  (line-separated
    "{"
    (apply line-separated (map stringify xs))
    "}"))

(defn assign-str [variable-name value]
  (expression-statement-str
    (str variable-name "=" (stringify value))))

(defn init-str [type variable-name value]
  (expression-statement-str
    (str type variable-name "=" (stringify value))))

#_(defn const [bindings]
  (str "const" Type x "=" initialValue))

#_(defn test* [x]
  ;; could be a boolean expression
  (str x "==" y)
  ;; or just a value
  (str x))


#_(defn conditional [test then else] )

(defn invoke-str [function-name args]
  (str function-name (param-list (map stringify args))))

(defn function-str [return-type name doc params body]
  (str
    (space-separated 'public 'static
                     return-type
                     name)
    (space-separated (param-list (for [param params]
                                   (str (or (some-> param meta :tag)
                                            "var")
                                        " "
                                        param)))
                     (stringify body))))

(defn operator-str [op & xs]
  (parens
    (str/join op (map stringify xs))))

(defn class-str [ns-name body]
  (let [parts (str/split (str ns-name) #"\.")
        package-name (str/join "." (butlast parts))
        class-name (last parts)]
    (line-separated
      (expression-statement-str (space-separated 'package package-name))
      (space-separated 'public 'class class-name
                       (stringify body)))))

(defn return-str [x]
  (space-separated 'return (stringify x)))

(defn while-str [condition body]
  (space-separated 'while (parens (stringify condition))
                   (stringify body)))

;; TODO
(defn for-str [& args]
  (space-separated 'for (parens args)))

(defn if-str
  ([test then]
   (line-separated
     (space-separated 'if (parens (stringify test)))
     (stringify then)))
  ([test then else]
   (line-separated
     (space-separated 'if (parens (stringify test)))
     (stringify then)
     'else
     (stringify else))))

;;;; This is the main entry point

(def str-fn-map
  {'j/class                class-str
   'j/operator             operator-str
   'j/function             function-str
   'j/invoke               invoke-str
   'j/init                 init-str
   'j/assign               assign-str
   'j/block                block-str
   'j/expression-statement expression-statement-str
   'j/return               return-str
   'j/while                while-str
   'j/for                  for-str
   'j/if                   if-str})

(def stringify
  (s/match
    (?x . !more ... :as ?form)
    (let [f (get str-fn-map ?x)]
      (if f
        (apply f !more)
        (throw (ex-info (str "Missing function: " ?x) {:form ?form}))))

    ?else (str ?else)))

(defn stringify-entry [form]
  (try
    (stringify form)
    (catch Exception ex
      (println "Inner form:")
      (pprint/pprint (:form (ex-data ex)))
      (println "Outer form:")
      (pprint/pprint form)
      (throw ex))))
