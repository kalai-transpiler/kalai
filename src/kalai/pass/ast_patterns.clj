(ns kalai.pass.ast-patterns
  (:require [meander.strategy.epsilon :as s]
            [meander.epsilon :as m]))

;;;;;; these are tools analyzer ast patterns of constructs that we support

(def language-concepts-sexp
  (s/rewrite
    ;; function
    (def
      ?name
      (fn*
        ((m/and [& ?params] (m/app meta {:tag ?return-type :doc ?doc}))
         ?body)))
    (function ?return-type ?name ?doc ?params ?body)

    ;; check the canonical form
    (def ?name ?value)
    (assignment ?name ?value)

    ;; lambda
    (fn*
      ;; optional name... do we want to support that? Does C++ have lambdas?
      ([& ?params]
       ?body))
    (lambda ?params ?body)))


#_(def clojure-concepts-sexp
    (s/rewrite
      (if ?test ?x ?y)
      (if ?test ?x ?y)))

;; entry point, nominally a file (seq of forms) for now
(def namespace-forms
  (s/rewrite
    ((ns ?ns-name . !clauses ...) . !forms ...)
    (namespace ?ns-name . (m/app language-concepts-sexp !forms) ...)

    ((do (clojure.core/in-ns ('quote ?ns-name)) & _)
     . !forms ...)
    (namespace ?ns-name . (m/app language-concepts-sexp !forms) ...)

    ?else ~(throw (ex-info "fail" {:input ?else}))
    ))
