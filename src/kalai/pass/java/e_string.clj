(ns kalai.pass.java.e-string
  (:require [meander.strategy.epsilon :as s]
            [meander.epsilon :as m]
            [clojure.string :as str]
            [camel-snake-kebab.core :as csk]
            [puget.printer :as puget]
            [clojure.java.io :as io]))

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
    (apply line-separated (map stringify (remove nil? xs)))
    "}"))

(defn assign-str [variable-name value]
  (statement (str variable-name " = " (stringify value))))

;; TODO: do we need an :object type?
(def kalai-type->java
  {:map     "PMap"
   :mmap    "HashMap"
   :set     "PSet"
   :mset    "HashSet"
   :vector  "PVector"
   :mvector "ArrayList"
   :bool    "boolean"
   :byte    "byte"
   :char    "char"
   :int     "int"
   :long    "long"
   :float   "float"
   :double  "double"
   :string  "String"
   :void    "void"
   :any     "Object"})

(defn java-type [t]
  (or (get kalai-type->java t)
      ;; TODO: breaking the rules for interop... is this a bad idea?
      (when t (pr-str t))
      "TYPE_MISSING"))

(defn box [s]
  (case s
    "int" "Integer"
    "char" "Character"
    "bool" "Boolean"
    (apply str (str/upper-case (first s)) (rest s))))

(def t-str
  (s/rewrite
    {?t [& ?ts]}
    ~(str (java-type ?t)
          \< (str/join \, (for [t ?ts]
                            (box (t-str t))))
          \>)

    ?t
    ~(str (java-type ?t))))

(defn type-modifiers [s mut global]
  (cond->> s
           (not mut) (space-separated "final")
           global (space-separated "static")))

(defn where [{:keys [file line column]}]
  (when file
    (str (.getName (io/file file)) ":" line ":" column)))

(defn maybe-warn-type-missing [t x]
  (when (str/includes? t "TYPE_MISSING")
    (binding [*print-meta* true]
      (println "WARNING: missing type detected" x
               (where (meta (:expr (meta x))))))))

(defn type-str [variable-name]
  (let [{:keys [t mut global]} (meta variable-name)]
    (-> (t-str t)
        (doto (maybe-warn-type-missing variable-name))
        (type-modifiers mut global))))

(defn init-str
  ([variable-name]
   ;; TODO: we could default "Objects" to null, otherwise Java won't compile
   ;; See (def ^{:t T} x nil) in type_alias.clj example
   (statement (space-separated (type-str variable-name)
                               variable-name)))
  ([variable-name value]
   (statement (space-separated (type-str variable-name)
                               variable-name "=" (stringify value)))))

(defn invoke-str [function-name & args]
  (str (if (str/includes? function-name "-")
         (csk/->camelCase function-name)
         function-name)
       (param-list (map stringify args))))

(defn function-str [name params body]
  (if (= '-main name)
    (do
      (assert (= '& (first params)) "Main method must have signature (defn -main [& args]...)")
      (str
        (space-separated 'public 'static
          'final
          'void
          'main)
        (space-separated (param-list [(space-separated "String[]" (second params))])
          (stringify body))))
    (str
      (space-separated 'public 'static
        (type-str params)
        (csk/->camelCase name))
      (space-separated (param-list (for [param params]
                                     (space-separated (type-str param) param)))
        (stringify body)))))

(defn operator-str
  ([op x]
   (str op (stringify x)))
  ([op x & xs]
   (parens
     (apply space-separated
            (interpose op (map stringify (cons x xs)))))))

(def std-imports
  "import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;")

(defn class-str [ns-name body]
  (let [parts (str/split (str ns-name) #"\.")
        package-name (csk/->camelCase (str/join "." (butlast parts)))
        class-name (csk/->camelCase (last parts))]
    (line-separated
      (statement (space-separated 'package package-name))
      std-imports
      (space-separated 'public 'class class-name
                       (stringify body)))))

(defn return-str [x]
  (space-separated 'return (stringify x)))

(defn while-str [condition body]
  (space-separated 'while (parens (stringify condition))
                   (stringify body)))

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
    "new" (str (if (symbol? class-name)
                 class-name
                 (doto (t-str class-name)
                   (maybe-warn-type-missing class-name)))
               (param-list args))))

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
          (throw (ex-info (str "Missing function: " ?x)
                          {:form ?form})))))

    (m/pred keyword? ?k)
    (pr-str (str ?k))

    (m/pred char? ?c)
    (str \' ?c \')

    nil
    "null"

    ?else
    (pr-str ?else)))

(defn stringify-entry [form]
  (try
    (stringify form)
    (catch Exception ex
      (println "Outer form:")
      (puget/cprint form)
      (throw ex))))
