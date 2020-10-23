(ns kalai.pass.kalai.c-operators
  (:require [kalai.util :as u]
            [meander.strategy.epsilon :as s]
            [meander.epsilon :as m]))

(def binary-operator
  '{clojure.lang.Numbers/add                    +
    clojure.lang.Numbers/addP                   +
    clojure.lang.Numbers/unchecked_add          +
    clojure.lang.Numbers/minus                  -
    clojure.lang.Numbers/minusP                 -
    clojure.lang.Numbers/unchecked_minus        -
    clojure.lang.Numbers/unchecked_int_subtract -
    clojure.lang.Numbers/multiply               *
    clojure.lang.Numbers/divide                 /
    clojure.lang.Util/equiv                     ==
    clojure.lang.Numbers/lt                     <
    clojure.lang.Numbers/lte                    <=
    clojure.lang.Numbers/gt                     >
    clojure.lang.Numbers/gte                    >=
    clojure.lang.Numbers/quotient               /
    clojure.lang.Numbers/remainder              %})

(def rewrite
  (s/bottom-up
    (s/rewrite
      ;; not really an operator, but seems like it belongs here
      (invoke clojure.lang.RT/intCast ?x)
      ~(clojure.lang.RT/intCast ?x)

      ;; binary operators
      (invoke (m/pred binary-operator ?op) ?x ?y)
      (operator (m/app binary-operator ?op) ?x ?y)

      ;; unitary operators
      (invoke not ?x)
      (operator '! ?x)

      (m/or
        (invoke (u/var ~#'inc) ?x)
        (invoke clojure.lang.Numbers/inc ?x)
        (invoke clojure.lang.Numbers/unchecked_inc ?x))
      (operator '++ ?x)

      (m/or
        (invoke (u/var ~#'dec) ?x)
        (invoke clojure.lang.Numbers/dec ?x)
        (invoke clojure.lang.Numbers/unchecked_dec ?x))
      (operator '-- ?x)

      ;; varity operators
      (invoke + . !args ...)
      (operator + . !args ...)

      (invoke and)
      true

      (invoke and . !args ...)
      (operator && . !args ...)

      (invoke or)
      false

      (invoke or . !args ...)
      (operator || . !args ...)

      ;;;

      ;;(invoke (u/var ~#'println) & ?more)
      ;;(invoke System.out.println & ?more)

      (invoke (u/var ~#'assoc) & ?more)
      (method put & ?more)

      (invoke (u/var ~#'dissoc) & ?more)
      (method remove & ?more)

      (invoke (u/var ~#'conj) & ?more)
      (method add & ?more)

      ;; TODO: this isn't really an operator..
      ;; TODO: temp variable creation must come first
      ;; philosophically, this is the fundamental difference between
      ;; imperative and functional... do we want to be opinionated about this?
      (m/and
        (invoke (u/var ~#'update) ?x ?k ?f & ?args)
        (m/let [?tmp (u/tmp-for ?x)]))
      (group
        (init ?tmp ?x)
        (method put ?tmp ?k
                (m/app rewrite (invoke ?f (method get ?tmp ?k) & ?args)))
        ?tmp)

      ?else
      ?else)))
