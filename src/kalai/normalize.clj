(ns kalai.normalize
  (:require [meander.epsilon :as m]
            [meander.strategy.epsilon :as s]
            [clojure.tools.analyzer.passes.jvm.emit-form :as e]
            [clojure.tools.analyzer.jvm :as aj]
            [clojure.string :as str]))

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

;; don't use multi-methods
;; chain pattern matchers?



;; (public String uPPerCase (String in)
;;         (.upperCase in))
;;; =>
;; public String uPPerCase(String in) {
;;     return in.upperCase();
;; }

(defn param-list [params]
  (str "(" (str/join "," params) ")"))

(defn statement [x]
  (str statement ";"))

(defn block* [xs]
  (str "{" xs "}"))

(defn assignments []
  ())

(defn const [bindings]
  (str "const" Type x "=" initialValue))

(defn unwrap [xs]
  (str/join " " xs))

(defn test* [x]
  ;; could be a boolean expression
  (str x "==" y)
  ;; or just a value
  (str x))


(defn conditional [test then else]
  )

(defn invocation [return-type name doc params body]
  )




;; Goals: recursive "organizing", delay stringification



;; strings above
;;; --------
;; -- nesting rewrites --
;; (block (block (statement s))) ~~> (block statement)
;;
;; (conditional test (statement a) (block (statement b))) ~~> (conditional test (block (statement a)) (block (statement b)))


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
    (f . !args ...)
    (invocation f !args)

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
    form))

;; entry point, nominally a file (seq of forms) for now
(def class
  (s/rewrite
    ((ns ?class-name) . !forms ...)
    (j/class
      (m/app top-level-form !form)
      ;; !forms can be statements or block

      ;;;(block (m/app statement !forms)) ...
      )))

;; don't need this
(def java-dispatch
  (s/match
    (function ?return-type ?name ?doc [!params ...] ?body)
    (java/function ?return-type ?name
                   (java/params . !param-type !param-names ...)
                   (java/block ?body))))

(def language-concepts-sexp
  (s/rewrite
    ;; function
    (def
      ?name
      (fn*
        ((m/and [& ?params] (m/app meta {:tag ?return-type :doc ?doc}))
         ?body)))
    (function ?return-type ?name ?doc ?params ?body)

    ;; lambda
    (fn*
      ;; optional name... do we want to support that? Does C++ have lambdas?
      ([& ?params]
       ?body))
    (lambda ?params ?body)))

(def clojure-concepts-sexp
  (s/rewrite
    (if ?test ?x ?y)
    (if ?test ?x ?y)))


(defn normalize' [ast]
  (e/emit-form ast))

(defn normalize [ast]
  ast)
