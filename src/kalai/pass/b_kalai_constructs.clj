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
  (s/rewrite
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

(def assignments
  (s/rewrite
    ;; let -> assignment
    (let* [!var !init] ?body)
    (do
      . (assignment !var (m/app inner-form !init)) ...
      (m/app inner-form ?body))

    ;; with-local-vars -> assignment
    (let* [!var (.setDynamic (clojure.lang.Var/create)) ..?n]
      (do
        (clojure.lang.Var/pushThreadBindings (clojure.core/hash-map . !var !init ..?n))
        (try
          ?body
          (finally (clojure.lang.Var/popThreadBindings)))))
    (do
      . (assignment !var (m/app inner-form !init)) ...
      (m/app inner-form ?body))

    (var-get ?x)
    ?x

    (var-set ?var ?x)
    (assignment ?var ?x)))

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

(def inner-form
  "Clauses must be ordered from most to least specific."
  (s/choice
    loops
    assignments
    operators
    conditionals
    misc
    s/pass))

(def top-level-form
  (s/rewrite
    ;; function
    (def ?name
      (fn*
        ((m/and [& ?params] (m/app meta {:tag ?return-type :doc ?doc}))
         . !body-forms ...)))
    (function ?return-type ?name ?doc ?params
              . (m/app inner-form !body-forms) ...)

    ;; check the canonical form
    (def ?name ?value)
    (assignment ?name ?value)

    ?else ~(throw (ex-info "fail" {:input ?else}))))

(def rewrite
  (s/rewrite
    ((do (clojure.core/in-ns ('quote ?ns-name)) & _)
     . !forms ...)
    (namespace ?ns-name . (m/app top-level-form !forms) ...)

    ?else ~(throw (ex-info "fail" {:input ?else}))))
