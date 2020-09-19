(ns kalai.pass.java.a-syntax
  (:require [meander.strategy.epsilon :as s]
            [meander.epsilon :as m]))

;;; -------- language constructs to syntax
;; expanded s-expressions below


;; what is an expression?
;; can occur in a statement, block, conditional etc...

;; what is a statement?
;; what expressions cannot be statements? block, x/if
;; expression semicolon
;;  3;
;;  x++;
;; if (x==1) x++;
;; if (x==1) {
;;    x++;
;; }
;; expression statements, declaration statements, and control flow statements


;; f(x+1, 3);
;; f(g(x+1), 3);

;; what is a block?
;; public static void f() { }
;; { {} }

;; what is an assignment?
;; what is a conditional?
;; what is a function definition?
;; what is an invocation

;; what expressions can be arguments to functions?
;; invocations, assignments maybe, literals, variables, ternaries maybe

;; expressions can contain other expressions
;; expressions cannot contain statements
;; statements can contain other statements
;; statements can contain expressions
;; block must contain statements (not expressions)

(def c (atom 0))
(defn gensym2 [s]
  (symbol (str s (swap! c inc))))

;; half Clojure half Java
(def expression
  (s/rewrite
    (m/and (persistent-vector . !x ...)
           (m/let [?t (gensym2 "tmp")]))
    (group
      (j/init PersistentVector ?t (j/new PersistentVector))
      . (j/expression-statement (j/method add ?t (m/app expression !x))) ...
      ?t)

    (m/and (persistent-map . !k !v ...)
           (m/let [?t (gensym2 "tmp")]))
    (group
      (j/init PersistentMap ?t (j/new PersistentMap))
      . (j/expression-statement (j/method put ?t !k !v)) ...
      ?t)

    (m/and (persistent-set . !x ...)
           (m/let [?t (gensym2 "tmp")]))
    (group
      (j/init PersistentSet ?t (j/new PersistentSet))
      . (j/expression-statement (j/method add ?t !x)) ...
      ?t)


    ;; operator usage
    (operator ?op ?x ?y)
    (j/operator ?op (m/app expression ?x) (m/app expression ?y))

    (operator ?op ?x ?y)
    (j/operator ?op (m/app expression ?x) (m/app expression ?y))

    ;; TODO: these shouldn't be necessary, kalai_constructs will have handled them... try removing
    (invoke clojure.core/deref ?x)
    (m/app expression ?x)

    ;; TEMPORARY
    (invoke atom ?x)
    (m/app expression ?x)

    ;; function invocation
    (invoke ?f . !args ...)
    (j/invoke ?f . (m/app expression !args) ...)

    (method ?method ?object . !args ...)
    (j/method ?method (m/app expression ?object) . (m/app expression !args) ...)

    ;; TODO: lambda function
    (lambda ?name ?docstring ?body)
    (j/lambda ?name ?docstring ?body)

    ;; conditionals as an expression must be ternaries, but ternaries cannot contain bodies
    (if ?condition ?then)
    (j/ternary (m/app expression ?condition) (m/app expression ?then) nil)

    (if ?condition ?then ?else)
    (j/ternary (m/app expression ?condition) (m/app expression ?then) (m/app expression ?else))

    ;; TODO: how to do this? maybe through variable assignment?
    (case ?x {& (m/seqable [!k [_ !v]] ...)})
    (j/switch (m/app expression ?x)
              (j/block . (j/case !k (j/expression-statement (m/app expression !v))) ...))

    ?x
    ?x))


(def init
  (s/rewrite
    (init ?mut ?type ?name)
    (j/init ?type ?name)

    (init ?mut ?type ?name ?x)
    (j/init ?type ?name (m/app expression ?x))))

(def statement
  (s/choice
    init
    (s/rewrite
      (return ?x)
      (j/expression-statement (j/return (m/app expression ?x)))

      (while ?condition . !body ...)
      (j/while (m/app expression ?condition)
               (j/block . (m/app statement !body) ...))

      (foreach ?type ?sym ?xs . !body ...)
      (j/foreach ?type ?sym (m/app expression ?xs)
                 (j/block . (m/app statement !body) ...))

      ;; conditional
      (if ?test ?then)
      (j/if (m/app expression ?test)
        (j/block (m/app statement ?then)))

      (if ?test ?then ?else)
      (j/if (m/app expression ?test)
        (j/block (m/app statement ?then))
        (j/block (m/app statement ?else)))

      (case ?x {& (m/seqable [!k [_ !v]] ...)})
      (j/switch (m/app expression ?x)
                (j/block . (j/case !k (j/expression-statement (m/app expression !v))) ...))

      (do . !xs ...)
      (j/block . (m/app statement !xs) ...)

      (assign ?name ?value)
      (j/assign ?name (m/app expression ?value))

      ?else (j/expression-statement (m/app expression ?else)))))

(def function
  (s/rewrite
    ;; function definition
    (function ?name ?return-type ?docstring ?params . !body ...)
    (j/function ?return-type ?name ?docstring ?params
                (j/block . (m/app statement !body) ...))))

(def top-level-form
  (s/choice
    function
    init
    (s/rewrite
      ?else ~(throw (ex-info "Expected a top level form" {:else ?else})))))

(def rewrite
  (s/rewrite
    (namespace ?ns-name . !forms ...)
    (j/class ?ns-name
             (j/block . (m/app top-level-form !forms) ...))

    ?else ~(throw (ex-info "Expected a namespace" {:else ?else}))))


#_(def z
    (s/rewrite
      (j/expression-statement
        (j/operator + 1 (group (assign x (invoke inc x)) x)))
      (m/let [?g ~(gensym "tmp")]
        (j/expression-statement
          (assign x (invoke inc x))
          (init ?g x)
          (j/operator + 1 ?g)))))

#_(def zz
    (s/rewrite
      (j/expression-statement . !x ... ?last)
      (j/block . !x ... (j/expression-statement ?last))))

;; bubble groups first? need to find expressions to do that done
#_(def expr
    (s/rewrite
      (!x ...)))

;; for swap, assignment actually does return a result, so no need for a group
;; for if, we need a group -- do we? what about ternaries?
;; need do and doto
