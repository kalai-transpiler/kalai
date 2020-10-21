(ns kalai.pass.rust.b-function-call
  (:require [kalai.util]
            [meander.strategy.epsilon :as s]))

(def rewrite
  (s/bottom-up
    (s/rewrite
      (r/invoke println & ?args)
      (r/invoke println! & ?args)

      (r/construct StringBuffer)
      (r/construct String)

      (r/method append (u/of-type StringBuffer ?this) ?x)
      (r/method push_str ?this ?x)

      (r/method length (u/of-type StringBuffer ?this))
      (r/method len ?this)

      (r/method toString (u/of-type StringBuffer ?this))
      ?this

      (r/method insert (u/of-type StringBuffer ?this) ?idx ?s2)
      (r/block
        (m/let [t (u/tmp StringBuffer)]
               (r/assign t ?this)
               (r/invoke truncate t ?idx)
               (r/invoke push_str t ?s2))))))
