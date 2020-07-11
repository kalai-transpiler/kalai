(ns kalai.pass.ast-patterns
  (:require [meander.strategy.epsilon :as s]
            [meander.epsilon :as m]))

;;;;;; these are tools analyzer ast patterns of constructs that we support

(def operator
  '{clojure.lang.Numbers/add                    +
    clojure.lang.Numbers/unchecked_int_subtract -
    clojure.lang.Numbers/multiply               *
    clojure.lang.Numbers/divide                 /
    clojure.lang.Numbers/lt                     <
    clojure.lang.Numbers/lte                    <=
    clojure.lang.Numbers/gt                     >
    clojure.lang.Numbers/gte                    >=})

(def inner-form
  "Clauses from most specific to least specific order."
  (s/rewrite
    (return ?x)
    (return (m/app inner-form ?x))

    (loop* [] (if ?conditional (do . !body ... (recur))))
    (while (m/app inner-form ?conditional)
      . (m/app inner-form !body) ...)

    ;;(loop* ?bindings ?body)
    ;;(loop ?bindings (m/app inner-form ?body))
    #_
    (let*
      [!var (.setDynamic (clojure.lang.Var/create)) ..?n]
      (do
        . (clojure.lang.Var/pushThreadBindings (clojure.core/hash-map !var !init)) ..?n
        (try
          ?body
          (finally (clojure.lang.Var/popThreadBindings)))))
    #_
    (do
      (assignment)
      (m/app inner-form ?body))

    (let* [!var !init] ?body)
    (do
      . (assignment !var (m/app inner-form !init)) ...
      (m/app inner-form ?body))

    ((m/pred operator ?op) ?x ?y)
    (operator (m/app operator ?op) (m/app inner-form ?x) (m/app inner-form ?y))

    (clojure.lang.Numbers/inc ?x)
    (operator + (m/app inner-form ?x) 1)

    (clojure.lang.Numbers/dec ?x)
    (operator - (m/app inner-form ?x) 1)

    (do . !more ...)
    (do . (m/app inner-form !more) ...)

    (if ?test ?then)
    (if (m/app inner-form ?test) (m/app inner-form ?then))

    (if ?test ?then ?else)
    (if (m/app inner-form ?test) (m/app inner-form ?then) (m/app inner-form ?else))

    ;; careful, this catches a lot!
    (?f . !args ...)
    (invoke ?f . (m/app inner-form !args) ...)

    ?else ?else))

(def top-level-form
  (s/rewrite
    ;; function
    (def
      ?name
      (fn*
        ((m/and [& ?params] (m/app meta {:tag ?return-type :doc ?doc}))
         . !body-forms ...)))
    (function ?return-type ?name ?doc ?params
              . (m/app inner-form !body-forms) ...)

    ;; check the canonical form
    (def ?name ?value)
    (assignment ?name ?value)

    ?else ~(throw (ex-info "fail" {:input ?else}))))


#_(def clojure-concepts-sexp
    (s/rewrite
      (if ?test ?x ?y)
      (if ?test ?x ?y)))

;; entry point, nominally a file (seq of forms) for now
(def namespace-forms
  (s/rewrite
    ((do (clojure.core/in-ns ('quote ?ns-name)) & _)
     . !forms ...)
    (namespace ?ns-name . (m/app top-level-form !forms) ...)

    ?else ~(throw (ex-info "fail" {:input ?else}))))
