(ns kalai.pass.rust.a-syntax
  (:require [kalai.pass.rust.util :as ru]
            [kalai.util :as u]
            [meander.strategy.epsilon :as s]
            [meander.epsilon :as m]))

(declare statement)

(def expression
  (s/rewrite
    ;; Data Literals

    ;;;; empty collections don't need a tmp variable (with new local block, etc.)
    (m/and (m/or [] {} #{})
           (m/pred empty?)
           (m/app (comp :t meta) ?t))
    ;;->
    (r/new ?t)

    ;;;; vector []

    ;; mutable vector ^{:t {:mvector [_]}} []
    (m/and [!x ...]
           ?expr
           (m/app (comp :t meta) (m/and ?t
                                        (m/pred :mvector)))
           (m/let [(m/or {_ [?value-t]}
                         (m/let [?value-t :any])) ?t
                   ?tmp (u/tmp ?t ?expr)]))
    ;;->
    (m/app
      #(u/preserve-type ?expr %)
      (r/block
        (r/init ?tmp (r/new ?t))
        . (r/expression-statement (r/method push ?tmp (m/app #(ru/wrap-value-enum ?value-t %) (m/app expression !x)))) ...
        ?tmp))

    ;; persistent vector ^{:t {:vector [_]}} []
    ;; or any other type on a vector literal
    (m/and [!x ...]
           ?expr
           (m/app (comp :t meta) ?t)
           (m/let [(m/or {_ [?value-t]}
                         (m/let [?value-t :any])) ?t]))
    ;;->
    (m/app
      #(u/preserve-type ?expr %)
      (m/app u/thread-second
             (r/new ~(if (= ?t :any)
                       {:vector [:any]}
                       ?t))
             . (r/method push_back
                         (m/app #(ru/wrap-value-enum ?value-t %)
                                (m/app expression !x))) ...
             ~(when (= ?t :any)
                '(r/invoke "kalai::BValue::from"))))

    ;;;; map {}

    ;; mutable map ^{:t {:mmap [_]}} {}
    (m/and {}
           ?expr
           (m/app u/sort-any-type ([!k !v] ...))
           (m/app (comp :t meta) (m/and ?t
                                        (m/pred :mmap)))
           (m/let [(m/or {_ [?key-t ?value-t]}
                         (m/let [?key-t :any
                                 ?value-t :any])) ?t
                   ?tmp (u/tmp ?t ?expr)]))
    ;;->
    (m/app
      #(u/preserve-type ?expr %)
      (r/block
        (r/init ?tmp (r/new ?t))
        . (r/expression-statement (r/method insert ?tmp
                                            (m/app #(ru/wrap-value-enum ?key-t %) (m/app expression !k))
                                            (m/app #(ru/wrap-value-enum ?value-t %) (m/app expression !v)))) ...
        ?tmp))

    ;; persistent map ^{:t {:map [_]}} {}
    ;; or any other type on a map literal
    (m/and {}
           ?expr
           (m/app u/sort-any-type ([!k !v] ...))
           (m/app (comp :t meta) ?t)
           (m/let [(m/or {_ [?key-t ?value-t]}
                         (m/let [?key-t :any
                                 ?value-t :any])) ?t]))
    ;;->
    (m/app
      #(u/preserve-type ?expr %)
      (m/app u/thread-second
             (r/new ~(if (= ?t :any)
                       {:map [:any :any]}
                       ?t))
             . (r/method insert
                         (m/app #(ru/wrap-value-enum ?key-t %) (m/app expression !k))
                         (m/app #(ru/wrap-value-enum ?value-t %) (m/app expression !v))) ...
             ~(when (= ?t :any)
                '(r/invoke "kalai::BValue::from"))))

    ;;;; set #{}

    ;; mutable set ^{:t {:mset [_]}} #{}
    (m/and #{}
           ?expr
           (m/app u/sort-any-type (!k ...))
           (m/app (comp :t meta) (m/and ?t
                                        (m/pred :mset)))
           (m/let [(m/or {_ [?key-t]}
                         (m/let [?key-t :any])) ?t
                   ?tmp (u/tmp ?t ?expr)]))
    ;;->
    (m/app
      #(u/preserve-type ?expr %)
      (r/block
        (r/init ?tmp (r/new ?t))
        . (r/expression-statement (r/method insert ?tmp (m/app #(ru/wrap-value-enum ?key-t %) (m/app expression !k)))) ...
        ?tmp))

    ;; persistent set ^{:t {:set [_]}} #{}
    ;; or any other type on a set literal
    (m/and #{}
           ?expr
           (m/app u/sort-any-type (!k ...))
           (m/app (comp :t meta) ?t)
           (m/let [(m/or {_ [?key-t]}
                         (m/let [?key-t :any])) ?t]))
    ;;->
    (m/app
      #(u/preserve-type ?expr %)
      (m/app u/thread-second
             (r/new ~(if (= ?t :any)
                       {:set [:any]}
                       ?t))
             . (r/method insert
                         (m/app #(ru/wrap-value-enum ?key-t %) (m/app expression !k))) ...
             ~(when (= ?t :any)
                '(r/invoke "kalai::BValue::from"))))

    ;; Interop
    (new ?c . !args ...)
    (r/new ?c . (m/app expression !args) ...)

    ;; operator usage
    (operator ?op . !args ...)
    (r/operator ?op . (m/app expression !args) ...)

    ;; function invocation
    (m/and (invoke ?f . !args ...)
           (m/app meta ?meta))
    (m/app with-meta
           (r/invoke ?f . (m/app expression !args) ...)
           ?meta)

    (method ?method ?object . !args ...)
    (r/method ?method (m/app expression ?object) . (m/app expression !args) ...)

    ;; TODO: lambda function
    (lambda ?name ?docstring ?body)
    (r/lambda ?name ?docstring ?body)

    ;; Note: Rust will not compile when conditionals as expressions don't have
    ;; an "else" branch (that is, only has a "then" branch).
    ;; Therefore, we should eventually deprecate this rule that only has a
    ;; "then" branch. The reason we still include it is so that the user will
    ;; eventually get a downstream Rust compiler error message.
    (if ?condition ?then)
    (r/if (m/app expression ?condition)
      (r/block (m/app expression ?then)))

    (if ?condition ?then ?else)
    (r/if (m/app expression ?condition)
      (r/block (m/app expression ?then))
      (r/block (m/app expression ?else)))

    ;; faithfully reproduce Clojure semantics for do as a collection of
    ;; side-effect statements and a return expression
    (do . !x ... ?last)
    (r/block
      . (m/app statement !x) ...
      (m/app expression ?last))

    ;; let

    ;; TODO: how to do this? maybe through variable assignment?
    (case ?x {& (m/seqable [!k [_ !v]] ...)} ?default)
    (r/match (m/app expression ?x)
             (r/block . (r/arm !k (m/app expression !v)) ...
                      (r/arm '_ (m/app expression ?default))))

    ?else
    ?else))

(def init
  (s/rewrite
    (init ?name)
    (r/init ?name)

    (init (m/and ?name (m/app meta {:t :any})) ?x)
    (r/init ?name (r/value (m/app expression ?x)))

    (init ?name ?x)
    (r/init ?name (m/app expression ?x))))

(def top-level-init
  (s/rewrite
    (init ?name)
    (r/init (m/app u/maybe-meta-assoc ?name :global true))

    (init ?name ?x)
    (r/init (m/app u/maybe-meta-assoc ?name :global true) (m/app expression ?x))))

(def statement
  (s/choice
    init
    (s/rewrite
      (return ?x)
      (r/expression-statement (r/return (m/app expression ?x)))

      (while ?condition . !body ...)
      (r/while (m/app expression ?condition)
               (r/block . (m/app statement !body) ...))

      (foreach ?sym ?xs . !body ...)
      (r/foreach ?sym (m/app expression ?xs)
                 (r/block . (m/app statement !body) ...))

      ;; conditional
      (if ?test ?then)
      (r/if (m/app expression ?test)
        (r/block (m/app statement ?then)))

      (if ?test ?then ?else)
      (r/if (m/app expression ?test)
        (r/block (m/app statement ?then))
        (r/block (m/app statement ?else)))

      (do . !xs ...)
      (r/block . (m/app statement !xs) ...)

      (assign ?name ?value)
      (r/assign ?name (m/app expression ?value))

      ?expr
      (r/expression-statement (m/app expression ?expr)))))

(def function
  (s/rewrite
    (function ?name ?params . !body ...)
    (r/function ?name ?params
                (r/block . (m/app statement !body) ...))))

(def top-level-form
  (s/choice
    function
    top-level-init
    (s/rewrite
      ?else ~(throw (ex-info "Expected a top level form" {:else ?else})))))

(def rewrite
  (s/rewrite
    (namespace ?ns-name . !forms ...)
    (r/module . (m/app top-level-form !forms) ...)

    ?else ~(throw (ex-info "Expected a namespace" {:else ?else}))))
