(ns kalai.pass.rust.ab-cast
  (:require [meander.strategy.epsilon :as s]
            [meander.epsilon :as m]))

(def rewrite
  (s/bottom-up
    (s/rewrite
      (m/and ?x
             (m/app meta {:t :any
                          :cast (m/pred some? ?t)}))
      (r/invoke ~(str "kalai::to_" (name ?t))
                ;; TODO: probably don't need to clone??
                (r/method clone ?x))

      (m/and ?x
             (m/app meta {:cast (m/pred some? ?t)}))
      (r/cast ?x ?t)

      ?else
      ?else)))
