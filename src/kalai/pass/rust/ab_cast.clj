(ns kalai.pass.rust.ab-cast
  (:require [meander.strategy.epsilon :as s]
            [meander.epsilon :as m]
            [kalai.pass.rust.e-string :as e-string]))

(def rewrite
  (s/bottom-up
    (s/rewrite
      ;; Support type cast to any via helper method from kalai.rs. Ex:
      ;; bool::from(x) which gives a bool from an x of type BValue
      (m/and ?x
             (m/app meta {:t :any
                          :cast (m/pred some? ?t)}))
      (r/invoke ~(str (e-string/init-rhs-t-str ?t) "::from") ?x)

      (m/and ?x
             (m/app meta {:cast (m/pred some? ?t)}))
      (r/cast ?x ?t)

      ?else
      ?else)))
