(ns kalai.pass.rust.ab-cast
  (:require [meander.strategy.epsilon :as s]
            [meander.epsilon :as m]
            [kalai.pass.rust.e-string :as e-string]
            [kalai.util :as u]))

(def rewrite
  (s/bottom-up
    (s/rewrite
      ;; Support type downcasting from `BValue`(`:any`) to a specific type
      ;; This style of casting is done by helper method from kalai.rs
      ;; Example: bool::from(x) which gives a bool from an x of type BValue
      (m/and ?x
             (m/app meta {:t :any
                          :cast (m/pred some? ?t)}))
      (r/invoke ~(str (e-string/init-rhs-t-str ?t) "::from") ?x)

      ;; Support type upcasting from specific type to `BValue`(`:any`)
      (m/and ?x
             (m/app meta {:cast :any}))
      (r/invoke ~(str (e-string/init-rhs-t-str :any) "::from") ?x)

      ;; This is Rust style for primitive type casting (x as T)
      (m/and ?x
             (m/app meta {:cast (m/pred some? ?t)}))
      (r/cast ?x ?t)

      ?else
      ?else)))
