(ns kalai.pass.python.b-syslib
  (:require [kalai.util :as u]
            [meander.strategy.epsilon :as s]))

(def rewrite
  (s/bottom-up
    (s/rewrite
      (p/construct StringBuffer)
      (p/construct String)

      (p/method append (u/match-type StringBuffer ?this) ?x)
      (p/operator + ?this ?x)

      (p/method length (u/match-type StringBuffer ?this))
      (p/invoke len ?this)

      (p/method toString (u/match-type StringBuffer ?this))
      ?this

      (p/method insert (u/match-type StringBuffer ?this) ?idx ?s2)
      (p/block
        (m/let [t (gensym "tmp")]
               (p/assign t ?this)
               (p/invoke truncate t ?idx)
               (p/operator + t ?s2))))))
