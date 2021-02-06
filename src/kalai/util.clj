(ns kalai.util
  (:require [meander.epsilon :as m]
            [meander.syntax.epsilon :as syntax]
            [meander.match.syntax.epsilon :as match]
            [puget.printer :as puget]
            [kalai.types :as types]))

(def c (atom 0))

(defn gensym2 [s]
  (symbol (str s (swap! c inc))))

(defn spy
  ([x] (spy x nil))
  ([x label]
   (println (str "Spy: " label))
   (flush)
   (binding [*print-meta* true]
     (doto x puget/pprint))))

(defn tmp [type expr]
  (with-meta (gensym2 "tmp") {:t type :expr expr :mut true}))

(defn tmp-for [expr]
  (tmp (types/get-type expr) expr))

;; TODO: we might not need this
(defn match-t?
  "Match the value for `t` in the :t key in the metadata map of `x`"
  [t x]
  (some-> x
          meta
          (#(= t (:t %)))))

;; Return whether `t` matches the value of :t of the metadata map
;; TODO: we might not need this
(m/defsyntax of-t [t x]
  (case (::syntax/phase &env)
    :meander/match
    `(match/pred #(match-t? ~t %) ~x)
    &form))

;; Return the value in :var in the metadata map
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
