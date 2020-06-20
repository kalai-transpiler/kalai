(ns kalai.pass.java-ast
  (:require [meander.strategy.epsilon :as s]))

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
  (m/rewrite
    ;; function invocation
    (?f . !args ...)
    (j/invocation ?f !args)

    ;; lambda function
    (lambda ?name ?docstring ?body)
    (j/lambda ?name ?docstring ?body)

    ;; are there really other statements?
    ?x
    (j/statement ?x)))

(def variable
  (m/rewrite
    [!x !y ...]
    (j/variable !x (j/assignment !y)) ...))

;; input Clojure form, output java taxonomy
;; Clojure form are kind of something statementy in Java
(def form
  (s/rewrite
    ;; set! is the assignment statement
    (set! ?variable ?expression)
    (j/assignment ?variable (m/app expression ?expression))

    ;; conditional
    ;; note: we don't know what to do with assignment, maybe disallow them
    (if ?test ?then . !?else ...)
    (j/if (test ?test)
      (j/block (m/app form ?then))
      (j/block (m/app form !?else)))

    ;; mutable variable scope
    (with-local-vars [!bindings ..?n] . !xs ..?m)
    (j/block (m/app variable !bindings) ..?n (m/app expression !xs) ..?m)

    ;; do form
    (do . !xs ...)
    (j/block (m/app statement !xs) ...)

    ;; let form
    (let [!bindings ..?n] . !xs ..?m)
    (j/block (m/app const !bindings) ..?n (m/app expression !xs) ..?m)))

(def top-level-form
  (s/choice
    (s/rewrite
      ;; function definition
      (function ?name ?docstring ?body)
      (j/function ?name ?docstring ?body))
    (s/rewrite
      (assignment ?name ?value)
      (j/assignment ?name ?value))))

;; entry point, nominally a file (seq of forms) for now
(def java-class
  (s/rewrite
    (namespace ?ns-name . !forms ...)
    (j/class ?ns-name
             (j/block . (m/app top-level-form !forms) ...))))
