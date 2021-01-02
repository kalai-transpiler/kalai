(ns kalai.pass.rust.e-string
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

(defn- comma-separated [& xs]
  (str/join ", " xs))

(defn- params-list [params]
  (parens (apply comma-separated params)))

(defn- args-list [args]
  (parens (apply comma-separated (map stringify args))))

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
(def kalai-type->rust
  {:map     "PMap"
   :mmap    "HashMap"
   :set     "PSet"
   :mset    "HashSet"
   :vector  "PVector"
   :mvector "Vec"
   :bool    "bool"
   :byte    "i8"
   :char    "char"
   :int     "i32"
   :long    "i64"
   :float   "f32"
   :double  "f64"
   :string  "String"
   :void    "()"
   :any     "Any"})

(defn rust-type [t]
  (or (get kalai-type->rust t)
      ;; TODO: breaking the rules for interop... is this a bad idea?
      (when t (pr-str t))
      "TYPE_MISSING"))

(def t-str
  (s/rewrite
    {?t [& ?ts]}
    ~(str (rust-type ?t)
          \< (str/join \, (for [t ?ts]
                            (t-str t)))
          \>)

    ?t
    ~(str (rust-type ?t))))


(defn type-modifiers [s mut global]
  (cond->> s
           mut (space-separated "mut")
           ;;global (space-separated "static")
           ))

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
   (statement (space-separated "let"
                               (str variable-name ":")
                               (type-str variable-name))))
  ([variable-name value]
   (statement (space-separated "let"
                               (str variable-name ":")
                               (type-str variable-name)
                               "="
                               (stringify value)))))

(defn invoke-str [function-name & args]
  (let [metameta (some-> function-name meta :var meta)]
    (if metameta
      (str (csk/->snake_case (str (:ns metameta))) "." (str (:name metameta))
           (args-list args))
      (str (if (str/includes? function-name "-")
             (csk/->snake_case function-name)
             function-name)
           (args-list args)))))

(defn function-str [name params body]
  (if (= '-main name)
    (do
      (assert (= '& (first params)) "Main method must have signature (defn -main [& args]...)")
      (str
        (space-separated 'public 'static 'final 'void 'main)
        (space-separated (params-list [(space-separated "String[]" (second params))])
                         (stringify body))))
    (str
      (space-separated "pub" "fn"
                       (csk/->camelCase name))
      (space-separated (params-list (for [param params]
                                      (space-separated (str param ":") (type-str param))))
                       "->"
                       (type-str params)
                       (stringify body)))))

(defn operator-str
  ([op x]
   (str op (stringify x)))
  ([op x & xs]
   (parens
     (apply space-separated
            (interpose op (map stringify (cons x xs)))))))

(def std-imports
  "use std:collections::HashMap;
use std::collections::HashSet;
use std::collections::Vec;")



(defn class-str [ns-name body]
  (let [parts (str/split (str ns-name) #"\.")
        package-name (str/lower-case (csk/->camelCase (str/join "." (butlast parts))))
        class-name (csk/->PascalCase (last parts))]
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
  (str (pr-str object) "." method (args-list args)))

(defn new-str [class-name & args]
  (space-separated
    "new" (str (if (symbol? class-name)
                 class-name
                 (doto (t-str class-name)
                   (maybe-warn-type-missing class-name)))
               (args-list args))))

;;;; This is the main entry point

(def str-fn-map
  {'r/class                class-str
   'r/operator             operator-str
   'r/function             function-str
   'r/invoke               invoke-str
   'r/init                 init-str
   'r/assign               assign-str
   'r/block                block-str
   'r/expression-statement expression-statement-str
   'r/return               return-str
   'r/while                while-str
   'r/for                  for-str
   'r/foreach              foreach-str
   'r/if                   if-str
   'r/ternary              ternary-str
   'r/switch               switch-str
   'r/case                 case-str
   'r/method               method-str
   'r/new                  new-str})

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
