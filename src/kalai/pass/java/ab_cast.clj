(ns kalai.pass.java.ab-cast
  (:require [meander.strategy.epsilon :as s]
            [meander.epsilon :as m]))

(def rewrite
  (s/bottom-up
    (s/rewrite
      (m/and ?x
             (m/app meta {:cast (m/pred some? ?t)}))
      (j/cast ?x ?t)

      ?else
      ?else)))
