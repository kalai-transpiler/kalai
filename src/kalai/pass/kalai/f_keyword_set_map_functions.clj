(ns kalai.pass.kalai.f-keyword-set-map-functions
  (:require [meander.strategy.epsilon :as s]
            [meander.epsilon :as m]))

(def rewrite
  (s/bottom-up
    (s/rewrite
      (invoke (m/pred keyword? ?k) ?x)
      (invoke clojure.lang.RT/get ?x ?k)

      (invoke (m/pred keyword? ?k) ?x ?default)
      (if (contains? ?x ?k)
        (invoke clojure.lang.RT/get ?x ?k)
        ?default)

      ?else ?else)))
