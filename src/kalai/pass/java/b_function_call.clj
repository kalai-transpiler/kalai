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

      ;; TODO: these should be (u/var)
      (j/invoke clojure.lang.RT/count ?x)
      (j/method (m/app count-for ?x) ?x)

      ;; TODO: need to do different stuff depending on the type
      (j/invoke clojure.lang.RT/nth ?x ?n)
      (j/method (m/app nth-for ?x) ?x ?n)

      (j/invoke clojure.lang.RT/get ?x ?k)
      (j/method get ?x ?k)

      (j/invoke (u/var ~#'contains?) ?coll ?x)
      (j/method containsKey ?coll ?x)

      (j/operator ==
                  (m/and (m/or (m/pred string?) (m/app meta {:t :string})) ?x)
                  (m/and (m/or (m/pred string?) (m/app meta {:t :string})) ?y))
      (j/method equals ?x ?y)

      (j/invoke (u/var ~#'assoc) & ?more)
      (j/method put & ?more)

      (j/invoke (u/var ~#'dissoc) & ?more)
      (j/method remove & ?more)

      (j/invoke (u/var ~#'conj) & ?more)
      (j/method add & ?more)

      (j/invoke (u/var ~#'inc) ?x)
      (j/operator + ?x 1)

      (j/invoke (u/var ~#'update) ?x ?k ?f & ?args)
      (j/method put ?x ?k
                (m/app rewrite (j/invoke ?f (j/method get ?x ?k) & ?args)))

      ?else
      ?else)))
