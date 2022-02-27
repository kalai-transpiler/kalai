(ns kalai.pass.java.a-syntax
  (:require [kalai.util :as u]
            [meander.strategy.epsilon :as s]
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

;;
;; Helper functions
;;

(defn thread-second
  [x & forms]
  "Returns forms (when given forms) like the 'thread-first' macro -> except that it puts each
  previous expression into the 3rd position of the new form/S-expression, not the second position
  like -> does."
  (loop [x x, forms forms]
    (if forms
      (let [form (first forms)
            threaded (if (seq? form)
                       (with-meta `(~(first form) ~(second form) ~x ~@(next (next form))) (meta form))
                       (list form x))]
        (recur threaded (next forms)))
      x)))


;;
;; Rules
;;


(declare statement)

;; half Clojure half Java
(def expression
  (s/rewrite
    ;; Data Literals

    ;;;; empty collections don't need a tmp
    (m/and (m/or [] {} #{})
           (m/pred empty?)
           (m/app (comp :t meta) ?t))
    ;;->
    (j/new ?t)

    ;;;; vector []

    ;; mutable vector ^{:t {:mvector [_]}} []
    (m/and [!x ...]
           ?expr
           (m/app (comp :t meta) (m/and ?t
                                        (m/pred :mvector)))
           (m/let [?tmp (u/tmp ?t ?expr)]))
    ;;->
    (group
      (j/init ?tmp (j/new ?t))
      . (j/expression-statement (j/method add ?tmp (m/app expression !x))) ...
      ?tmp)

    ;; persistent vector ^{:t {:vector [_]}} []
    (m/and [!x ...]
           ?expr
           (m/app (comp :t meta) (m/and ?t
                                        (m/pred :vector))))
    ;;->
    (m/app thread-second
           (j/new ?t)
           . (j/method addLast
                       (m/app expression !x)) ...)

    ;;;; map {}

    ;; mutable map ^{:t {:mmap [_]}} {}
    (m/and {}
           ?expr
           (m/app u/sort-any-type ([!k !v] ...))
           (m/app (comp :t meta) (m/and ?t
                                        (m/pred :mmap)))
           (m/let [?tmp (u/tmp ?t ?expr)]))
    ;;->
    (group
      (j/init ?tmp (j/new ?t))
      . (j/expression-statement (j/method put ?tmp
                                          (m/app expression !k)
                                          (m/app expression !v))) ...
      ?tmp)

    ;; persistent map ^{:t {:map [_]}} {}
    (m/and {}
           ?expr
           (m/app u/sort-any-type ([!k !v] ...))
           (m/app (comp :t meta) (m/and ?t
                                        (m/pred :map))))
    ;;->
    (m/app thread-second
           (j/new ?t)
           . (j/method put
                       (m/app expression !k)
                       (m/app expression !v)
                       io.lacuna.bifurcan.Maps.MERGE_LAST_WRITE_WINS) ...)


    ;;;; set #{}

    ;; mutable set ^{:t {:mset [_]}} #{}
    (m/and #{}
           ?expr
           (m/app u/sort-any-type (!k ...))
           (m/app (comp :t meta) (m/and ?t
                                        (m/pred :mset)))
           (m/let [?tmp (u/tmp ?t ?expr)]))
    ;;->
    (group
      (j/init ?tmp (j/new ?t))
      . (j/expression-statement (j/method add ?tmp (m/app expression !k))) ...
      ?tmp)

    ;; persistent set ^{:t {:set [_]}} #{}
    (m/and #{}
           ?expr
           (m/app u/sort-any-type (!k ...))
           (m/app (comp :t meta) (m/and ?t
                                        (m/pred :set))))
    ;;->
    (m/app thread-second
           (j/new ?t)
           . (j/method add
                       (m/app expression !k)) ...)


    ;; Interop
    (new ?c . !args ...)
    (j/new ?c . (m/app expression !args) ...)

    ;; operator usage
    (operator ?op . !args ...)
    (j/operator ?op . (m/app expression !args) ...)

    ;; function invocation
    (invoke ?f . !args ...)
    (j/invoke ?f . (m/app expression !args) ...)

    (method ?method ?object . !args ...)
    (j/method ?method (m/app expression ?object) . (m/app expression !args) ...)

    ;; TODO: lambda function
    (lambda ?name ?docstring ?body)
    (j/lambda ?name ?docstring ?body)

    ;; conditionals as an expression must be ternaries, but ternaries cannot contain bodies
    ;;(if ?condition ?then)
    ;;(j/ternary (m/app expression ?condition) (m/app expression ?then) nil)

    ;;(if ?condition ?then ?else)
    ;;(j/ternary (m/app expression ?condition) (m/app expression ?then) (m/app expression ?else))

    ;; when a conditional appears as an expression, we need a tmp variable,
    ;; so we create a group.
    (m/and (if ?condition ?then)
           (m/let [?tmp (u/tmp-for ?then)]))
    (group
      (j/init (m/app u/maybe-meta-assoc ?tmp :mut true))
      (j/if (m/app expression ?condition)
        (j/block (j/assign ?tmp (m/app expression ?then))))
      ?tmp)

    (m/and (if ?condition ?then ?else)
           (m/let [?tmp (u/tmp-for ?then)]))
    (group
      (j/init (m/app u/maybe-meta-assoc ?tmp :mut true))
      (j/if (m/app expression ?condition)
        (j/block (j/assign ?tmp (m/app expression ?then)))
        (j/block (j/assign ?tmp (m/app expression ?else))))
      ?tmp)

    ;; faithfully reproduce Clojure semantics for do as a collection of
    ;; side-effect statements and a return expression
    (do . !x ... ?last)
    (group
      . (m/app statement !x) ...
      (m/app expression ?last))

    ;; let

    ;; TODO: how to do this? maybe through variable assignment?
    (case ?x {& (m/seqable [!k [_ !v]] ...)})
    (j/switch (m/app expression ?x)
              (j/block . (j/case !k (j/expression-statement (m/app expression !v))) ...))

    ?else
    ?else))

(def init
  (s/rewrite
    (init ?name)
    (j/init ?name)

    (init ?name ?x)
    (j/init ?name (m/app expression ?x))))

(def top-level-init
  (s/rewrite
    (init ?name)
    (j/init (m/app u/maybe-meta-assoc ?name :global true))

    (init ?name ?x)
    (j/init (m/app u/maybe-meta-assoc ?name :global true) (m/app expression ?x))))

(def statement
  (s/choice
    init
    (s/rewrite
      (return ?x)
      (j/expression-statement (j/return (m/app expression ?x)))

      (while ?condition . !body ...)
      (j/while (m/app expression ?condition)
               (j/block . (m/app statement !body) ...))

      (foreach ?sym ?xs . !body ...)
      (j/foreach ?sym (m/app expression ?xs)
                 (j/block . (m/app statement !body) ...))

      ;; conditional
      (if ?test ?then)
      (j/if (m/app expression ?test)
        (j/block (m/app statement ?then)))

      (if ?test ?then ?else)
      (j/if (m/app expression ?test)
        (j/block (m/app statement ?then))
        (j/block (m/app statement ?else)))

      (case ?x {& (m/seqable [!k [_ !v]] ...)} ?default)
      (j/switch (m/app expression ?x)
                (j/block . (j/case !k (j/expression-statement (m/app expression !v))) ...
                         (j/default ?default)))

      (do . !xs ...)
      (j/block . (m/app statement !xs) ...)

      (assign ?name ?value)
      (j/assign ?name (m/app expression ?value))

      (m/and
        (m/or
          (assign & _)
          (operator ++ _)
          (operator -- _)
          (method & _)
          (invoke & _)
          (new & _))
        ?expr)
      (j/expression-statement (m/app expression ?expr))

      ;; Java does not allow other expressions as statements
      ?else
      nil)))

(def function
  (s/rewrite
    ;; function definition
    (function ?name ?params . !body ...)
    (j/function ?name ?params
                (j/block . (m/app statement !body) ...))))

(def top-level-form
  (s/choice
    function
    top-level-init
    (s/rewrite
      ?else ~(throw (ex-info "Expected a top level form" {:else ?else})))))

(def rewrite
  (s/rewrite
    (namespace ?ns-name . !forms ...)
    (j/class ?ns-name
             (j/block . (m/app top-level-form !forms) ...))

    ?else ~(throw (ex-info "Expected a namespace" {:else ?else}))))
