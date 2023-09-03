(ns kalai.pass.java.e-string
  (:require [meander.strategy.epsilon :as s]
            [meander.epsilon :as m]
            [clojure.string :as str]
            [camel-snake-kebab.core :as csk]
            [puget.printer :as puget]
            [clojure.java.io :as io]
            [kalai.pass.java.util :as ju]
            [kalai.util :as u]))

(declare stringify)

;;;; These are helpers

(defn- parens [x]
  (str "(" x ")"))

(defn- comma-separated [& xs]
  (str/join ", " xs))

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
(def kalai-type->java
  {:map      "io.lacuna.bifurcan.Map"
   :mmap     "HashMap"
   :set      "io.lacuna.bifurcan.Set"
   :mset     "HashSet"
   :vector   "io.lacuna.bifurcan.List"
   :mvector  "ArrayList"
   :function "java.util.Function"
   :bool     "boolean"
   :byte     "byte"
   :char     "char"
   :int      "int"
   :long     "long"
   :float    "float"
   :double   "double"
   :string   "String"
   :void     "void"
   :any      "Object"})

(defn java-type [t]
  (or (get kalai-type->java t)
      ;; TODO: breaking the rules for interop... is this a bad idea?
      (when t (pr-str t))
      "TYPE_MISSING"))

(defn box [s]
  (case s
    "int" "Integer"
    "long" "Long"
    "char" "Character"
    "bool" "Boolean"
    "float" "Float"
    "double" "Double"
    "byte" "Byte"
    "short" "Short"
    s))

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

(defn typed-param [param]
  (space-separated (type-str param)
                   (csk/->camelCase param)))

(defn params-list [params]
  (parens (apply comma-separated
                 (for [param params]
                   (typed-param param)))))

(defn init-str
  ([variable-name]
   ;; TODO: we could default "Objects" to null, otherwise Java won't compile
   ;; See (def ^{:t T} x nil) in type_alias.clj example
   (statement (typed-param variable-name)))
  ([variable-name value]
   (statement (space-separated (typed-param variable-name)
                               "=" (stringify value))))
  ([a b & args]
   (throw (ex-info (str "Init has more than 2 args:\n" (str/join \newline args))
                   {:form args}))))

(defn invoke-str [function-name & args]
  (str (if (symbol? function-name)
         (ju/fully-qualified-function-identifier-str function-name ".")
         (stringify function-name))
       (args-list args)))

(defn function-str [name params body]
  (if (= '-main name)
    (do
      (assert (= '& (first params)) "Main method must have signature (defn -main [& args]...)")
      (str
        (space-separated 'public 'static 'final 'void 'main)
        (space-separated (parens (space-separated "String[]" (second params)))
                         (stringify body))))
    (str
      (space-separated 'public 'static
                       (type-str params)
                       (csk/->camelCase name))
      (space-separated (params-list params)
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
import java.util.ArrayList;
import java.util.stream.Collectors;
import kalai.Kalai;")

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

(defn case-str [v expr]
  (str (space-separated "case" (stringify v) ":" (stringify expr))
       \newline "break;"))

(defn default-str [expr]
  (str (space-separated "default" ":" (stringify expr))
       \newline "break;"))

(defn method-str [method object & args]
  (str (stringify object) "." method (args-list args)))

(defn new-str [class-name & args]
  (space-separated
    "new" (str (if (symbol? class-name)
                 class-name
                 (doto (t-str class-name)
                   (maybe-warn-type-missing class-name)))
               (args-list args))))

(defn cast-str [v t]
  (str (parens (t-str t)) (stringify v)))

(defn lambda-str [params body]
  (space-separated (args-list params) "->" (stringify body)))

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
   'j/foreach              foreach-str
   'j/if                   if-str
   'j/ternary              ternary-str
   'j/switch               switch-str
   'j/case                 case-str
   'j/default              default-str
   'j/method               method-str
   'j/new                  new-str
   'j/cast                 cast-str
   'j/lambda               lambda-str})

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

    (m/pred #(instance? Long %) ?x)
    (str ?x "L")

    (m/pred float? ?x)
    (str ?x "F")

    nil
    "null"

    ;; identifier
    (m/pred #(and (symbol? %)
                  (str/includes? (str %) "-"))
            ?s)
    (csk/->camelCase (str ?s))

    ?else
    (pr-str ?else)))

(defn stringify-entry [form]
  (try
    (str (stringify form) \newline)
    (catch Exception ex
      (println "Outer form:")
      (puget/cprint form)
      (throw ex))))
