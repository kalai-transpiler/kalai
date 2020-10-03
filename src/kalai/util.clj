(ns kalai.util
  (:require [meander.epsilon :as m]
            [meander.syntax.epsilon :as syntax]
            [meander.match.syntax.epsilon :as match]))

(defn match-type-fn [t x]
  (some-> x
          meta
          :tag
          (= t)))

(m/defsyntax match-type [t x]
  (case (::syntax/phase &env)
    :meander/match
    `(match/pred #(match-type-fn ~t %) ~x)

    &form))
