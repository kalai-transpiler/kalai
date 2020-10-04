(ns kalai.pass.java.e-string
  (:require [meander.strategy.epsilon :as s]
            [meander.epsilon :as m]
            [clojure.string :as str]
            [kalai.util :as u]
            [puget.printer :as puget]))

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

(defn statement [s]
  (str s ";"))


;;;; These are what our symbols should resolve to

(defn expression-statement-str [x]
  (statement (stringify x)))

(defn block-str [& xs]
  (line-separated
    "{"
    (apply line-separated (map stringify xs))
    "}"))

(defn assign-str [variable-name value]
  (statement (str variable-name " = " (stringify value))))

(def ktypes
  {"map"       "Map"
   "kmap"      "Map"
   "set"       "Set"
   "kset"      "Set"
   "vector"    "Vector"
   "kvector"   "Vector"
   "kbyte"     "byte"
   "kchar"     "char"
   "kint"      "int"
   "klong"     "long"
   "kfloat"    "float"
   "kdouble"   "double"
   "kstring"   "string"})

(def java-types
  {java.util.Map     "Map"
   java.util.Set     "Set"
   java.util.Vector  "Vector"
   java.lang.Long    "long"
   java.lang.Integer "int"
   java.lang.Float   "float"
   java.lang.Double  "double"
   java.lang.String  "string"})

(defn ktype* [s]
  (or (get ktypes s)
      s))

(defn ktype [t]
  (or
    (cond
      (string? t) (ktype* t)
      (symbol? t) (ktype* (name t))
      (keyword? t) (ktype* (name t))
      (class? t) (ktype* (get java-types t))
      ;; TODO: We've lost the source line:column of the form at this point,
      ;; it would be nice to preserve it for better error reporting
      :else (println "WARNING: missing type detected"))
    "TYPE_MISSING"))

(defn box [s]
  (case s
    "int" "Integer"
    "char" "Character"
    (str/capitalize s)))

(def type-str*
  (s/rewrite
    {?t [& ?ts]}
    ~(str (ktype ?t)
          \< (str/join \, (for [t ?ts]
                            (type-str* [:boxed t])))
          \>)
    [:boxed ?t]
    ~(str (box (ktype ?t)))
    ?t
    ~(str (ktype ?t))))

;; Types are allowed to flow through the pipeline as metadata
(defn type-str [x]
  (let [{:keys [t tag]} (meta x)]
    (type-str* (or t tag "TYPE_MISSING"))))

(defn init-str
  ([variable-name]
   (statement (space-separated (type-str variable-name) variable-name)))
  ([variable-name value]
   (statement (space-separated (type-str variable-name) variable-name "=" (stringify value)))))

#_(defn const [bindings]
    (str "const" Type x "=" initialValue))

#_(defn test* [x]
    ;; could be a boolean expression
    (str x "==" y)
    ;; or just a value
    (str x))


#_(defn conditional [test then else])

(defn invoke-str [function-name & args]
  (str function-name (param-list (map stringify args))))

(defn function-str [name params body]
  (str
    (space-separated 'public 'static
                     (type-str params)
                     name)
    (space-separated (param-list (for [param params]
                                   (space-separated (type-str param) param)))
                     (stringify body))))

(defn operator-str [op & xs]
  (parens
    (apply space-separated
           (interpose op (map stringify xs)))))

(defn class-str [ns-name body]
  (let [parts (str/split (str ns-name) #"\.")
        package-name (str/join "." (butlast parts))
        class-name (last parts)]
    (line-separated
      (statement (space-separated 'package package-name))
      (space-separated 'public 'class class-name
                       (stringify body)))))

(defn return-str [x]
  (space-separated 'return (stringify x)))

(defn while-str [condition body]
  (space-separated 'while (parens (stringify condition))
                   (stringify body)))

;; TODO



(defn foreach-str [sym xs body]
  (space-separated 'for (parens (space-separated (type-str sym) sym ":" (stringify xs)))
                   (stringify body)))

(defn for-str [initialization termination increment body]
  (space-separated 'for (parens (str/join "; " (map stringify [initialization termination increment])))
                   (stringify body)))

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

(defn ternary-str
  [test then else]
  (parens
    (space-separated
      (stringify test)
      "?" (stringify then)
      ":" (stringify else))))

(defn switch-str
  [x clauses]
  (space-separated 'switch (parens (stringify x))
                   (stringify clauses)))

(defn case-str [x then]
  (str (space-separated "case" (stringify x) ":" (stringify then))
       \newline "break;"))

(defn method-str [method object & args]
  (str object "." method (param-list (map stringify args))))

(defn new-str [class-name & args]
  (space-separated
    "new" (str class-name (param-list args))))

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
   'j/foreach              foreach-str
   'j/if                   if-str
   'j/ternary              ternary-str
   'j/switch               switch-str
   'j/case                 case-str
   'j/method               method-str
   'j/new                  new-str})

(def stringify
  (s/match
    (?x . !more ... :as ?form)
    (let [f (get str-fn-map ?x)]
      (if f
        (apply f !more)
        (do
          (println "Inner form:")
          (puget/cprint ?form)
          (throw (ex-info (str "Missing function: " ?x) {:form ?form})))))

    (m/pred keyword? ?k) (pr-str (str ?k))

    ?else (pr-str ?else)))

(defn stringify-entry [form]
  (try
    (stringify form)
    (catch Exception ex
      (println "Outer form:")
      (puget/cprint form)
      (throw ex))))
