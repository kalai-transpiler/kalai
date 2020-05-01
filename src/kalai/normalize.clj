(ns kalai.normalize
  (:require [meander.epsilon :as m]
            [meander.strategy.epsilon :as s]
            [clojure.tools.analyzer.passes.jvm.emit-form :as e]
            [clojure.tools.analyzer.jvm :as aj]))

(def language-concepts-ast
  (s/rewrite
    ;; function
    {:op :def
     ;; TODO: enforce top-level?
     :name ?name
     :meta {:doc ?doc}
     :init {:expr {:op :fn
                   ;; TODO: do all target languages have multi-arity semantics?
                   :methods [{:params ?params
                              :body ?body}]}}}
    (function ?name ?doc ?params ?body)

    ;; lambda
    {:op :fn
     :methods [{:params ?params
                :body ?body}]}
    (lambda ?params ?body)

    ;; variables

    ;; TODO: disallow combining def and fn without defn???

    ))

(def clojure-concepts-ast
  "See http://clojure.github.io/tools.analyzer.jvm/spec/quickref.html"
  (s/rewrite
    {:op :binding
     :name ?name
     :local ?local}
    (binding ?name)

    {:op :case
     :test ?test
     :tests [!case-test ...]
     :thens [!case-then ...]
     :default ?default
     ;; TODO: other stuff!
     }
    (case ?test . (m/app normalize !case-test) (m/app normalize !case-then) ...)

    ))

(def language-concepts-sexp
  (s/rewrite
    ;; function
    (def
      (m/and
        ?name
        (m/app meta {:doc ?doc}))
      (fn*
        ([& ?params]
         ?body)))
    (function ?name ?doc ?params ?body)

    ;; lambda
    (fn*
      ;; optional name... do we want to support that? Does C++ have lambdas?
      ([& ?params]
       ?body))
    (lambda ?params ?body)))

(defn normalize' [ast]
  (e/emit-form ast))

(defn normalize [ast]
  ast)
