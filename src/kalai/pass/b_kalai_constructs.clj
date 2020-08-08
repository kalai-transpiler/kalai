(ns kalai.pass.b-kalai-constructs
  (:require [meander.strategy.epsilon :as s]
            [meander.epsilon :as m]))

(declare inner-form)

(def operator
  '{clojure.lang.Numbers/add                    +
    clojure.lang.Numbers/unchecked_int_subtract -
    clojure.lang.Numbers/multiply               *
    clojure.lang.Numbers/divide                 /
    clojure.lang.Numbers/lt                     <
    clojure.lang.Numbers/lte                    <=
    clojure.lang.Numbers/gt                     >
    clojure.lang.Numbers/gte                    >=})

(def operators
  (s/rewrite
    ((m/pred operator ?op) ?x ?y)
    (operator (m/app operator ?op) (m/app inner-form ?x) (m/app inner-form ?y))

    (clojure.lang.Numbers/inc ?x)
    (operator + (m/app inner-form ?x) 1)

    (clojure.lang.Numbers/dec ?x)
    (operator - (m/app inner-form ?x) 1)))

(def loops
  (fn [x] x)
  #_(s/rewrite
    ;; while -> while
    (loop* [] (if ?conditional (do . !body ... (recur))))
    (while (m/app inner-form ?conditional)
      . (m/app inner-form !body) ...)

    ;; dotimes -> (let [...] (while ...))
    (let* [?auto (clojure.lang.RT/longCast ?n)]
      (loop* [?sym 0]
        (if (clojure.lang.Numbers/lt ?sym ?auto)
          (do . !body ... (recur (clojure.lang.Numbers/unchecked_inc ?sym))))))
    (let* [?sym 0]
      (while (operator < ?sym ?auto)
        . (m/app inner-form !body) ...
        (operator ++ ?sym)))

    ;; doseq -> foreach
    (loop* [?seq (clojure.core/seq ?xs)
            ?chunk nil
            ?chunkn 0
            ?i 0]
      (if (clojure.lang.Numbers/lt ?i ?chunkn)
        (let* [?sym (.nth ?chunk ?i)]
          (do . !body ... (recur ?seq ?chunk ?chunkn (clojure.lang.Numbers/unchecked_inc ?i))))
        (let* [?as (clojure.core/seq ?seq)]
          (if ?as
            (let* [?bs ?as]
              (if (clojure.core/chunked-seq? ?bs)
                (let* [?cs (clojure.core/chunk-first ?bs)]
                  (recur
                    (clojure.core/chunk-rest ?bs)
                    ?cs
                    (clojure.lang.RT/intCast (clojure.lang.RT/count ?cs))
                    (clojure.lang.RT/intCast 0)))
                (let* [?sym (clojure.core/first ?bs)]
                  (do . !body ... (recur (clojure.core/next ?bs) nil 0 0)))))))))
    (foreach ?sym ?xs . !body ...)

    ;; loop -> ???
    ;;(loop* ?bindings ?body)
    ;;(loop ?bindings (m/app inner-form ?body))
    ))

(def ref-symbol?
  #{'atom
    'ref
    'agent
    'clojure.core/atom
    'clojure.core/ref
    'clojure.core/agent})

(def ref-form?
  (s/rewrite
    (m/pred ref-symbol? ?x)
    ?x

    ?else false))

(def assignments
  (s/rewrite

    ;; TODO: delete me when choice works
    (do . !more ...)
    (do . (m/app inner-form !more) ...)

    ;; with-local-vars
    (let* [!sym (.setDynamic (clojure.lang.Var/create)) ..?n]
      (do
        (clojure.lang.Var/pushThreadBindings (clojure.core/hash-map . !sym !init ..?n))
        (try
          ?body
          (finally (clojure.lang.Var/popThreadBindings)))))
    ;;->
    (do
      . (init !sym (m/app inner-form !init)) ..?n
      (m/app inner-form ?body))

    ;; (atom 3)
    ;; let
    (let* [!sym !init ...] ?body)
    ;;->
    (do
      . (init (m/app (fn hint-const [sym]
                       ;;; TODO not quite right yet
                       (if (ref-form? !init)
                         sym
                         (with-meta sym {:const true})))
                     !sym)
              (m/app inner-form !init)) ...
      (m/app inner-form ?body))

    ;; def
    (def ?name ?value)
    (init ?name (m/app inner-form ?value))

    ;; def with no value
    (def ?name)
    (init ?name)

    ;; TODO: Clojure has nuanced concepts of state...
    ;; should we boil them all down to assignment, provide equivalent abstractions, or only support 1?
    ;; currently boiling them all down to basic assignment.

    (var-get ?x)
    ?x

    (var-set ?var ?x)
    (assign ?var ?x)

    (reset! ?a ?x)
    (assign ?a ?x)

    (ref-set ?r ?x)
    (assign ?r ?x)

    (swap! ?a ?f & ?args)
    (assign ?a (invoke ?f ?a & ?args))

    (alter ?r ?f & ?args)
    (assign ?r (invoke ?f ?r & ?args))

    (alter-var-root ?v ?f & ?args)
    (assign ?v (invoke ?f ?v & ?args))

    (send ?a ?f & ?args)
    (assign ?a (invoke ?f ?a & ?args))

    (send-off ?a ?f & ?args)
    (assign ?a (invoke ?f ?a & ?args))

    (m/pred ref-symbol? ?x)
    ?x

    (deref ?x)
    ?x

    (clojure.core/deref ?x)
    ?x))

(def conditionals
  (s/rewrite
    (if ?test ?then)
    (if (m/app inner-form ?test) (m/app inner-form ?then))

    (if ?test ?then ?else)
    (if (m/app inner-form ?test) (m/app inner-form ?then) (m/app inner-form ?else))))

(def misc
  (s/rewrite
    ;; TODO: do we still need this? return gets annotated later...
    (return ?x)
    (return (m/app inner-form ?x))

    ;; TODO: should these be unwrapped here?
    (do . !more ...)
    (do . (m/app inner-form !more) ...)

    ;; invoke
    ;; careful, this catches a lot!
    (?f . !args ...)
    (invoke ?f . (m/app inner-form !args) ...)))

;; TODO: choice does not work as expected, only the first rule works
(def inner-form
  "Ordered from most to least specific."
  (s/choice
    assignments
    loops
    operators
    conditionals
    misc
    s/pass))

;; TODO: use this everywhere!
(defn always-meta [x]
  (or (meta x) {}))

(def top-level-form
  (s/rewrite
    ;; TODO: only matching single arity is a problem.... maybe convert Kalai functions to multi-arity
    ;; defn
    (def ?name
      (fn*
        ((m/and [& ?params] (m/app always-meta {:tag ?return-type :doc ?doc}))
         . !body-forms ...)))
    ;;->
    (function ?return-type ?name ?doc ?params
              . (m/app inner-form !body-forms) ...)

    ;; def
    (def ?name ?value)
    (init ?name (m/app inner-form ?value))

    ;; def with no value
    (def ?name)
    (variable ?name)

    ?else ~(throw (ex-info "fail" {:else ?else}))))


;; takes a sequence of forms, returns a single form
(def rewrite
  (s/rewrite
    ;; ns
    ((do (clojure.core/in-ns ('quote ?ns-name)) & _)
     . !forms ...)
    ;;->
    (namespace ?ns-name . (m/app top-level-form !forms) ...)

    ?else ~(throw (ex-info "fail" {:else ?else}))))
