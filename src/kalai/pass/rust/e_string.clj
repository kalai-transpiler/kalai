(ns kalai.pass.rust.e-string
  (:require [meander.strategy.epsilon :as s]
            [meander.epsilon :as m]
            [clojure.string :as str]
            [camel-snake-kebab.core :as csk]
            [puget.printer :as puget]
            [clojure.java.io :as io]
            [kalai.types :as types]
            [kalai.util :as u]
            [kalai.pass.rust.util :as ru]
            [clojure.string :as string])
  (:import (clojure.lang IMeta Keyword)
           (java.util Map Set Vector)))

;; forward declare stringfy to satisfy mutual recursion of `stringfy` and its helpers

(declare stringify)

;;;; These are helpers

(defn- parens [x]
  (str "(" x ")"))

(defn- comma-separated [xs]
  (str/join ", " xs))

(defn- params-list [params]
  (parens (comma-separated params)))

(defn- stringify-arg [arg]
  (let [{:keys [mut ref]} (meta arg)]
    (str (when ref "&")
         (when mut "mut ")
         (stringify arg))))

(defn- args-list [args]
  (parens (comma-separated (map stringify-arg args))))

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
  {:map     "rpds::HashTrieMap"
   :mmap    "std::collections::HashMap"
   :set     "rpds::HashTrieSet"
   :mset    "std::collections::HashSet"
   :vector  "rpds::Vector"
   ;; TODO: does this depend on whether it's a {:t {:vector [:some-primitive]}} vs. {:t {:vector [:any]}} ? How is this being used instead of t-str?
   :mvector "std::vec::Vec"
   :bool    "bool"
   :byte    "i8"
   :char    "char"
   :int     "i32"
   :long    "i64"
   :float   "f32"
   :double  "f64"
   :string  "String"
   :void    "()"
   :any     "kalai::BValue"
   :option  "Option"})

(defn kalai-primitive-type->rust
  [t]
  (or ({:mvector "kalai::Vector"
        :mset    "kalai::Set"
        :mmap    "kalai::Map"} t)
      (kalai-type->rust t)
      types/BAD-TYPE_CAST-STR))

;; Forward declaration of `t-str` to break cycle of references.
;; We expect this not to create an infinite loop in practice, otherwise
;; the types specified in types/lang-types-mapping is not configured correctly.
(declare t-str)

(defn rust-type
  "`t` is either the :t value we would find in metadata (in the S-exprs) or
  would be data of the same form for a target-language specific type
  (ex: `:usize`)."
  [t]
  (or (get kalai-type->rust t)
      ;; TODO: breaking the rules for interop... is this a bad idea?
      (when t
        ;; TODO: refactor (move) the `get-in` call upstream in the pipeline so that
        ;; values for `:t` conform to a narrow consistent spec.
        (if-let [custom-rust-type-data (get-in types/lang-type-mappings [:kalai.emit.langs/rust t])]
          (t-str custom-rust-type-data)
          (if (keyword? t)
            (name t)
            (pr-str t))))
      types/TYPE-MISSING-STR))

(def t-str
  "?t may be a symbol, but could also be a
  a data structure, just as we might find in `:t ` of the metadata
  map upstream in the S-exprs."
  (s/rewrite
    {:mvector [:any]}
    "kalai::Vector"

    {:mset [:any]}
    "kalai::Set"

    ;; TODO: Do we want to support {:map [ (not :any) :any]) and vice versa {:map [:any (not :any)]} ?
    {:mmap [:any :any]}
    "kalai::Map"

    {?t [& ?ts]}
    ~(str (rust-type ?t)
          \< (str/join \, (for [t ?ts]
                            (t-str t)))
          \>)

    ?t
    ~(rust-type ?t)))

(def init-rhs-t-str
  "?t may be a symbol, but could also be a
  a data structure, just as we might find in `:t ` of the metadata
  map upstream in the S-exprs."
  (s/rewrite
    {:mvector [:any]}
    "kalai::Vector"

    {:mset [:any]}
    "kalai::Set"

    ;; TODO: Do we want to support {:map [ (not :any) :any]) and vice versa {:map [:any (not :any)]} ?
    {:mmap [:any :any]}
    "kalai::Map"

    {?t [& ?ts]}
    ~(rust-type ?t)

    ?t
    ~(rust-type ?t)))


(defn where [{:keys [file line column]}]
  (when file
    (str (.getName (io/file file)) ":" line ":" column)))

(defn maybe-warn-type-missing [t x]
  (when (str/includes? t types/TYPE-MISSING-STR)
    (binding [*print-meta* true]
      (println "WARNING: missing type detected" x
               (where (meta (:expr (meta x))))))))

(defn type-str [variable-name]
  (let [{:keys [t]} (meta variable-name)]
    (-> (t-str t)
        (doto (maybe-warn-type-missing variable-name)))))

(defn variable-name-type-str [variable-name]
  (let [{:keys [t mut ref]} (meta variable-name)]
    ;; let mut char_vec: &Vec<char> = ;
    ;; let char_vec: &Vec<char> = ;
    ;; let mut char_vec: std::vec::Vec<char> = ;
    (str (when mut "mut ") (ru/identifier variable-name)
         ;; Rust has type inference, so we can leave temp variable types off
         (when t
           (str ": " (when ref "&") (type-str variable-name))))))

(defn cast-str [identifier t]
  (space-separated (stringify identifier) "as" (t-str t)))

(defn init-str
  ([variable-name]
   (init-str variable-name nil))
  ([variable-name value]
   (let [{:keys [global]} (meta variable-name)]
     (if global
       (line-separated
         "lazy_static::lazy_static! {"
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
  (str (ru/fully-qualified-function-identifier-str function-name)
       (args-list args)))

(defn param-str [param]
  ;; fn f(char_vec: &mut std::vec::Vec<char>) {...
  ;; fn f(char_vec: &Vec<char>) {...
  ;; NOT POSSIBLE? (or doesn't make sense?): fn f(char_vec: mut std::vec::Vec<char>) {...
  (let [{:keys [mut ref]} (meta param)]
    (if (= param 'self)
      (str (when ref "&")
           (when mut "mut ")
           "self")
      (space-separated (str (ru/identifier param) ":")
                       (str (when ref "&")
                            (when mut "mut ")
                            (type-str param))))))

(defn function-str [name params body]
  (if (= '-main name)
    (do
      (assert (= '& (first params)) "Main method must have signature (defn -main [& args]...)")
      (str
        (space-separated 'pub 'fn 'main (params-list [])
                         (line-separated "{"
                                         (str "let " (ru/identifier (second params)) ": std::vec::Vec<String> = std::env::args().collect();")
                                         (stringify body)
                                         "}"))))
    (str
      (space-separated "pub" "fn"
                       (ru/identifier name))
      (space-separated (params-list (for [param params]
                                      (param-str param)))
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

(def std-imports "use crate::kalai;\nuse crate::kalai::PMap;")

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
  ;; numbers are not explicitly typed for matches, the match value should generally be an expression
  (space-separated 'match (if (number? x) x (stringify x))
                   (stringify clauses)))

(defn arm-str [x then]
  ;; Clojure does not allow number literals of a specific type in case statements (it forces longs to ints).
  ;; Therefore numbers cannot be explicitly typed for match arms
  ;; and we prevent the default behavior of emitting specify type literals
  (str (space-separated (if (number? x) x (stringify x)) "=>" (str (stringify then) ","))))

(defn method-str [method object & args]
  (str (stringify object) "." method
    (when-let [t (-> method meta :t)]
      (str "::<" (t-str t) ">"))
    (args-list args)))

(defn new-str [t & args]
  (str (if (symbol? t)
         t
         (doto (init-rhs-t-str t)
           (maybe-warn-type-missing t)))
       "::new"
       (args-list args)))

(defn literal-str [s]
  (pr-str s))

(defn lambda-str [args body]
  (str "|" (comma-separated (map ru/identifier args)) "|"
       (stringify body)))

(defn ref-str [s]
  (if (and (instance? IMeta s)
           (:ref (meta s)))
    ;; this prevents a ref from becoming a double-ref.
    ;; we probably don't want to be doing this?
    (stringify s)
    (str "&" (stringify s))))

(defn deref-str [s]
  (str "*" (stringify s)))

(defn range-str [start-idx end-idx]
  ;; do not call `stringify` on the values for start-idx, end-idx
  ;; because they are literals and we do not want to interfere with the
  ;; Rust compiler's implicit conversion to type `usize`.
  (str start-idx ".." end-idx))

(defn kalai-value-types [t]
  (if (map? t)
    (case (-> t keys first)
      :map "PMap"
      :mmap "MMap"
      :set "PSet"
      :mset "MSet"
      :vector "PVector"
      :mvector "MVector"
      nil)
    (case t
      :bool "Bool"
      :byte "Byte"
      :int "Int"
      :long "Long"
      :float "Float"
      :double "Double"
      :string "String"
      nil)))

(defn value-type
  "This is specifically for our custom rust Value enum for heterogeneous collections
  x is a value that may have meta data on it inticating a type.
  x may be a primitive value or may need to be wrapped in the special rust Value enum."
  [x]
  (if (nil? x)
    "Null"
    (if (instance? IMeta x)
      (let [{:keys [t]} (meta x)]
        (kalai-value-types t))
      (condp instance? x
        Byte "Byte"
        Boolean "Bool"
        Float "Float"
        Double "Double"
        Integer "Int"
        Long "Long"
        Keyword "String"
        String "String"
        Map "MMap"
        Set "MSet"
        Vector "MVector"
        nil))))

(defn value-str
  "Specifically for the Value enum for heterogeneous collections.
  x is a value that may have meta data on it inticating a type.
  x may be a primitive value or may need to be wrapped in the special rust Value enum."
  [x]
  (if (value-type x)
    (str "kalai::BValue::from" (parens (stringify x)))
    (stringify x)))

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
   'r/lambda               lambda-str
   'r/cast                 cast-str
   'r/ref                  ref-str
   'r/deref                deref-str
   'r/range                range-str
   'r/value                value-str})

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

    (m/pred #(instance? Long %) ?x)
    (str ?x "i64")

    (m/pred #(instance? Integer %) ?x)
    (str ?x "i32")

    (m/pred #(instance? Double %) ?x)
    (str ?x "f64")

    (m/pred #(instance? Float %) ?x)
    (str ?x "f32")

    (m/pred string? ?s)
    (str "String::from(" (pr-str ?s) ")")

    nil
    "kalai::BValue::from(kalai::NIL)"

    ;; identifier
    (m/pred symbol? ?s)
    (ru/identifier ?s)

    ?else
    (pr-str ?else)))

(defn stringify-entry [form]
  (try
    (stringify form)
    (catch Exception ex
      (println "Outer form:")
      (puget/cprint form)
      (throw ex))))
