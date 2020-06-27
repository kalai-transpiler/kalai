(ns kalai.pass.ast-patterns
  (:require [meander.strategy.epsilon :as s]
            [meander.epsilon :as m]))

;;;;;; these are tools analyzer ast patterns of constructs that we support

(def operator
  '{clojure.lang.Numbers/add                    +
    clojure.lang.Numbers/unchecked_int_subtract -
    clojure.lang.Numbers/multiply               *
    clojure.lang.Numbers/divide                 /})

(def inner-form
  (s/rewrite
    (return ?x)
    (return (m/app inner-form ?x))

    ((m/pred operator ?op) ?x ?y)
    (operator (m/app operator ?op) (m/app inner-form ?x) (m/app inner-form ?y))

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
