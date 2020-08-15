(ns kalai.pass.java1-syntax
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

;; half Clojure half Java
(def expression
  (s/rewrite
    ;; operator usage
    (operator ?op ?x ?y)
    (j/operator ?op (m/app expression ?x) (m/app expression ?y))

    (operator ?op ?x ?y)
    (j/operator ?op (m/app expression ?x) (m/app expression ?y))

    (invoke clojure.core/deref ?x)
    (m/app expression ?x)

    ;; TEMPORARY
    (invoke atom ?x)
    (m/app expression ?x)

    ;; function invocation
    (invoke ?f . !args ...)
    (j/invoke ?f [(m/app expression !args) ...])

    ;; TODO:
    ;; lambda function
    (lambda ?name ?docstring ?body)
    (j/lambda ?name ?docstring ?body)

    ;; are there really other statements?
    ?x
    ?x))

(def variable
  (s/rewrite
    [?var ?init]
    (j/variable ?var (j/assignment ?init))
    ?else ~(throw (ex-info "FAIL" {:else ?else}))))

(def statement
  (s/rewrite
    ;; return
    (return ?x)
    (j/expression-statement (j/return (m/app expression ?x)))

    ;;loop
    ;;(loop ?bindings ?body)
    ;;(j/while true (j/block ?body))

    ;; while
    (while ?condition . !body ...)
    (j/while (m/app expression ?condition)
             (j/block . (m/app statement !body) ...))

    ;; foreach
    (foreach & ?more)
    (j/for & ?more)

    ;; set! is the assignment statement
    (set! ?variable ?expression)
    (j/expression-statement (j/assignment ?variable (m/app expression ?expression)))

    ;; conditional
    ;; note: we don't know what to do with assignment, maybe disallow them
    (if ?test ?then)
    (j/if (m/app expression ?test)
      (j/block (m/app statement ?then)))

    (if ?test ?then ?else)
    (j/if (m/app expression ?test)
      (j/block (m/app statement ?then))
      (j/block (m/app statement ?else)))

    ;; mutable variable scope
    #_(with-local-vars [!bindings ..?n] . !xs ..?m)
    #_(j/block . (m/app variable [!var !init]) ..?n
               (m/app statement ?body))

    ;; TODO: should probably make children expression-statements
    ;; do form
    (do . !xs ...)
    (j/block . (m/app statement !xs) ...)

    (init (m/and ?name (m/app meta {:t ?t :tag ?type})))
    (j/init ~(or ?t ?type) ?name)

    (init (m/and ?name (m/app meta {:t ?t :tag ?type})) ?value)
    (j/init ~(or ?t ?type) ?name (m/app expression ?value))

    (assign ?name ?value)
    (j/assign ?name (m/app expression ?value))

    ?else (j/expression-statement (m/app expression ?else))))

(def function
  (s/rewrite
    ;; function definition
    (function ?name ?return-type ?docstring ?params . !body ...)
    (j/function ?return-type ?name ?docstring ?params
                (j/block . (m/app statement !body) ...))))

(def init
  (s/rewrite
    (init (m/and ?name (m/app meta {:t ?type})))
    (j/init ?type ?name)

    (init (m/and ?name (m/app meta {:t ?type})) (m/app expression ?value))
    (j/init ?type ?name (m/app expression ?value))))

(def top-level-form
  (s/choice
    function
    init
    (s/rewrite
      ?else ~(throw (ex-info "FAIL" {:else ?else})))))

(def rewrite
  (s/rewrite
    (namespace ?ns-name . !forms ...)
    (j/class ?ns-name
             (j/block . (m/app top-level-form !forms) ...))

    ?else ~(throw (ex-info "FAIL" {:else ?else}))))
