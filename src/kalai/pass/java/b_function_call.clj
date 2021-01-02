(ns kalai.pass.java.b-function-call
  (:require [kalai.util :as u]
            [meander.strategy.epsilon :as s]
            [meander.epsilon :as m]))

;; TODO: user extension point, is dynamic var good?
;; can it be more data driven?
(def ^:dynamic *user*)

;; If we do this before syntax, we can remove j/invoke... is that good or bad?
;; (do we match on the Java syntax, or the Kalai syntax?)


(defn nth-for [x]
  (if (= (:t (meta x)) :string)
    'charAt
    'get))

(defn count-for [x]
  (m/rewrite (:t (meta x))
    {(m/pred #{:mmap :map :mset :set :mvector :vector}) (m/pred some?)} 'size
    ?else 'length))

(def rewrite
  (s/bottom-up
    (s/rewrite
      (j/invoke (u/var ~#'println) & ?more)
      (j/invoke System.out.println & ?more)

      (j/invoke clojure.lang.RT/count ?x)
      (j/method (m/app count-for ?x) ?x)

      ;; TODO: need to do different stuff depending on the type
      (j/invoke clojure.lang.RT/nth ?x ?n)
      (j/method (m/app nth-for ?x) ?x ?n)

      (j/invoke clojure.lang.RT/get ?x ?k)
      (j/method get ?x ?k)

      (j/operator ==
                  (m/and (m/or (m/pred string?) (m/app meta {:t :string})) ?x)
                  (m/and (m/or (m/pred string?) (m/app meta {:t :string})) ?y))
      (j/method equals ?x ?y)

      ?else
      ?else)))
