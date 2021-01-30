(ns kalai.pass.rust.e-string
  (:require [meander.strategy.epsilon :as s]
            [meander.epsilon :as m]
            [clojure.string :as str]
            [camel-snake-kebab.core :as csk]
            [puget.printer :as puget]
            [clojure.java.io :as io])
  (:import (clojure.lang IMeta)))

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
    (apply line-separated (map stringify xs))
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

(defn where [{:keys [file line column]}]
  (when file
    (str (.getName (io/file file)) ":" line ":" column)))

(defn maybe-warn-type-missing [t x]
  (when (str/includes? t "TYPE_MISSING")
    (binding [*print-meta* true]
      (println "WARNING: missing type detected" x
               (where (meta (:expr (meta x))))))))

(defn type-str [variable-name]
  (let [{:keys [t]} (meta variable-name)]
    (-> (t-str t)
        (doto (maybe-warn-type-missing variable-name)))))

(defn variable-name-type-str [variable-name]
  (let [{:keys [mut]} (meta variable-name)]
    (str (when mut "mut ") (csk/->snake_case variable-name) ": "
         (type-str variable-name))))

(defn init-str
  ([variable-name]
   (init-str variable-name nil))
  ([variable-name value]
   (let [{:keys [global]} (meta variable-name)]
     (if global
       (line-separated
         "lazy_static! {"
         (statement (space-separated "static ref"
                                     (variable-name-type-str variable-name)
                                     "="
                                     (stringify value)))
         "}")
       (statement (space-separated "let"
                                   (variable-name-type-str variable-name)
                                   "="
                                   (stringify value)))))))

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
        (space-separated 'fn 'main (params-list [])
                         (line-separated "{"
                                         (str "let " (csk/->snake_case (second params)) ": Vec<String> = env::args().collect();")
                                         (stringify body)
                                         "}"))))
    (str
      (space-separated "pub" "fn"
                       (csk/->snake_case name))
      (space-separated (params-list (for [param params]
                                      (space-separated (str (csk/->snake_case param) ":")
                                                       (type-str param))))
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
  "#[macro_use]
extern crate lazy_static;
use std::collections::HashMap;
use std::collections::HashSet;
use std::vec::Vec;
use std::env;")

(defn module-str [& forms]
  (apply line-separated
    std-imports
    (map stringify forms)))

(defn return-str [x]
  (space-separated 'return (stringify x)))

(defn while-str [condition body]
  (space-separated 'while (stringify condition)
                   (stringify body)))

(defn foreach-str [sym xs body]
  (space-separated 'for (space-separated sym "in" (stringify xs))
                   (stringify body)))

(defn if-str
  ([test then]
   (line-separated
     (space-separated 'if (stringify test))
     (stringify then)))
  ([test then else]
   (line-separated
     (space-separated 'if (stringify test))
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

(defn match-str
  [x clauses]
  (space-separated 'match (stringify x)
                   (stringify clauses)))

(defn arm-str [x then]
  (str (space-separated (stringify x) "=>" (str (stringify then) ","))))

(defn method-str [method object & args]
  (str (stringify object) "." method (args-list args)))

(defn new-str [t & args]
  (str (if (symbol? t)
         t
         (doto (-> t (keys) (first) (rust-type))
           (maybe-warn-type-missing t)))
       "::new"
       (args-list args)))

(defn literal-str [s]
  (pr-str s))

(defn ref-str [s]
  (if (and (instance? IMeta s)
           (:ref (meta s)))
    (stringify s)
    (str "&" (stringify s))))

;;;; This is the main entry point

(def str-fn-map
  {'r/module               module-str
   'r/operator             operator-str
   'r/function             function-str
   'r/invoke               invoke-str
   'r/init                 init-str
   'r/assign               assign-str
   'r/block                block-str
   'r/expression-statement expression-statement-str
   'r/return               return-str
   'r/while                while-str
   'r/foreach              foreach-str
   'r/if                   if-str
   'r/ternary              ternary-str
   'r/match                match-str
   'r/arm                  arm-str
   'r/method               method-str
   'r/new                  new-str
   'r/literal              literal-str
   'r/ref                  ref-str})

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
    (str "String::from(" (pr-str (str ?k)) ")")

    (m/pred char? ?c)
    (str \' ?c \')

    (m/pred string? ?s)
    (str "String::from(" (pr-str ?s) ")")

    nil
    "()"

    ;; identifier
    (m/pred symbol? ?s)
    (let [s-str (str ?s)
          snake-case (csk/->snake_case s-str)]
      (if (= \_ (first s-str))
        (str \_ snake-case)
        snake-case))

    ?else
    (pr-str ?else)))

(defn stringify-entry [form]
  (try
    (stringify form)
    (catch Exception ex
      (println "Outer form:")
      (puget/cprint form)
      (throw ex))))
