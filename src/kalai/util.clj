(ns kalai.util
  (:require [meander.epsilon :as m]
            [meander.syntax.epsilon :as syntax]
            [meander.match.syntax.epsilon :as match]
            [puget.printer :as puget]
            [kalai.types :as types]
            [camel-snake-kebab.core :as csk]
            [camel-snake-kebab.internals.string-separator :as csk-ss]))

(def c
  "The counter used by `gensym2` to implement the auto-increment number suffix."
  (atom 0))

(defn gensym2
  "Returns a symbol for an identifier whose name needs to be unique. The name is prefixed by `s`,
and uses an auto-incrementing number as a suffix for uniqueness."
  ([]
   (gensym2 "X__"))
  ([s]
   (symbol (str s (swap! c inc)))))

(defn spy
  ([x] (spy x nil))
  ([x label]
   (println (str "Spy: " label))
   (flush)
   (binding [*print-meta* true]
     (doto x puget/pprint))))

(defn tmp
  "Creates a unique symbol (named via `gensym2`) with the metadata necessary for
creating a temporary mutable variable.
`expr` is, to the extent known, what we want to assign to the tmp variable."
  [type expr]
  (with-meta (gensym2 "tmp") {:t type :expr expr :mut true}))

(defn tmp-for [expr]
  (tmp (types/get-type expr) expr))

(defn match-t?
  "Match the value for `t` in the :t key in the metadata map of `x`"
  [t x]
  (some-> x
          meta
          (#(= t (:t %)))))

;; Return whether `t` matches the value of :t of the metadata map
(m/defsyntax of-t [t x]
  (case (::syntax/phase &env)
    :meander/match
    `(match/pred #(match-t? ~t %) ~x)
    &form))

;; Matches a var
(m/defsyntax var [v]
  (case (::syntax/phase &env)
    :meander/match
    `(m/app meta {:var ~v})
    &form))

(defn maybe-meta-assoc
  "If v is truthy, sets k to v in meta of x"
  ([x k v]
   (if v
     (with-meta x (assoc (meta x) k v))
     x))
  ([x k v & more]
   {:pre [(even? (count more))]}
   (apply maybe-meta-assoc (maybe-meta-assoc x k v) more)))

(defn sort-any-type
  "Provides a deterministic ordering of the entries/elements of the provided collection.
Maps are ordered by their keys. Numbers come before strings, and numbers and strings
 are thusly sorted independently before concatenating in the return value."
  [coll]
  (if (map? coll)
    (let [{numbers true non-numbers false} (group-by (comp number? key) coll)]
      (concat (sort-by key numbers) (sort-by (comp str key) non-numbers)))
    (let [{numbers true non-numbers false} (group-by number? coll)]
      (concat (sort numbers) (sort-by str non-numbers)))))

(defn generic-split
  "Modify camel-snake-kebab behavior to prevent segmenting identifier strings
 when a letter is followed by a number. Ex: `get_f32` should not be `get_f_32`."
  [ss]
  (let [cs (mapv csk-ss/classify-char ss)
        ss-length (.length ^String ss)]
    (loop [result (transient []), start 0, current 0]
      (let [next (inc current)
            result+new (fn [end]
                         (if (> end start)
                           (conj! result (.substring ^String ss start end))
                           result))]
        (cond (>= current ss-length)
              (or (seq (persistent! (result+new current)))
                  ;; Return this instead of an empty seq:
                  [""])

              (= (nth cs current) :whitespace)
              (recur (result+new current) next next)

              (let [[a b c] (subvec cs current)]
                ;; This expression is not pretty,
                ;; but it compiles down to sane JavaScript.
                (or (and (not= a :upper)  (= b :upper))
                    ;; We changed the following line from the original to support not
                    ;; putting underscores inside something like "u64" or "tmp1".
                    (and (= a :number) (not= b :number))
                    (and (= a :upper) (= b :upper) (= c :lower))))
              (recur (result+new next) next next)

              :else
              (recur result start next))))))

;; TODO: should use this in Java string emitters wherever csk/->snake-case is used
;; so that we are consistent with identifiers across Java and rust

(defn ->snake_case
  "Convert to snake_case using our override behavior for `generic_split`."
  [s]
  (with-redefs [csk-ss/generic-split generic-split]
    (csk/->snake_case s)))


(defn thread-second
  [x & forms]
  "Returns forms (when given forms) like the 'thread-first' macro -> except that it puts each
  previous expression into the 3rd position of the new form/S-expression, not the second position
  like -> does."
  (loop [x x, forms forms]
    (if forms
      (let [form (first forms)
            threaded (if (seq? form)
                       (with-meta `(~(first form) ~(second form) ~x ~@(next (next form))) (meta form))
                       (if form
                         (list form x)
                         x))]
        (recur threaded (next forms)))
      x)))

(defn preserve-type
  "Replace `expr` with `replacement-expr`, but ensure the type information
   for `expr` is preserved on `replacement-expr`."
  [expr replacement-expr]
  (with-meta
    replacement-expr
    (or (meta expr)
        (when-let [t (get types/java-types (type expr))]
          {:t t}))))

(def binary-operator
  '{clojure.lang.Numbers/add                    +
    clojure.lang.Numbers/addP                   +
    clojure.lang.Numbers/unchecked_add          +
    clojure.lang.Numbers/minus                  -
    clojure.lang.Numbers/minusP                 -
    clojure.lang.Numbers/unchecked_minus        -
    clojure.lang.Numbers/unchecked_int_subtract -
    clojure.lang.Numbers/multiply               *
    clojure.lang.Numbers/divide                 /
    clojure.lang.Util/equiv                     ==
    clojure.lang.Numbers/lt                     <
    clojure.lang.Numbers/lte                    <=
    clojure.lang.Numbers/gt                     >
    clojure.lang.Numbers/gte                    >=
    clojure.lang.Numbers/quotient               /
    clojure.lang.Numbers/remainder              %})

(def operator-symbols
  (set (vals binary-operator)))
