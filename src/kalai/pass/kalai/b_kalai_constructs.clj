(ns kalai.pass.kalai.b-kalai-constructs
  (:require [meander.strategy.epsilon :as s]
            [meander.epsilon :as m]
            [kalai.util :as u]))

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

    (clojure.lang.Numbers/unchecked_inc ?x)
    (operator + (m/app inner-form ?x) 1)

    (clojure.lang.Numbers/dec ?x)
    (operator - (m/app inner-form ?x) 1)))

(def data-literals
  "Data literals are mostly unchanged,
  except that data literals may contain expressions inside them.
  Metadata from the original object must propagate."
  (s/rewrite
    (m/and [!k ...]
           (m/app meta ?meta))
    (m/app with-meta
           [(m/app inner-form !k) ...]
           ?meta)

    (m/and (m/map-of !k !v)
           (m/app meta ?meta))
    (m/app with-meta
           (m/map-of (m/app inner-form !k) (m/app inner-form !v))
           ?meta)

    (m/and (m/set-of !k)
           (m/app meta ?meta))
    (m/app with-meta
           (m/set-of (m/app inner-form !k))
           ?meta)))

(def ref-symbol?
  '#{atom
     ref
     agent
     clojure.core/atom
     clojure.core/ref
     clojure.core/agent})

(defn ref-form? [x]
  (and (seq? x)
       (ref-symbol? (first x))))

(defn as-init
  [?name ?x mutability]
  (list 'init
        (u/set-meta ?name :mut
                    (or (= mutability :mutable)
                        (ref-form? ?x)))
        (inner-form ?x)))

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
    (group
      (init ~(with-meta ?sym {:t 'int
                              :mut true})
            0)
      (while (operator < ?sym ?n)
        . (m/app inner-form !body) ...
        (assign ?sym (operator + ?sym 1))))

    ;; TODO: test with
    ;;;; (doseq [x [1 2]] (println x) (println x))
    ;; doseq -> foreach
    (loop* [?seq (clojure.core/seq ?xs)
            ?chunk nil
            ?chunkn 0
            ?i 0]
      (if (clojure.lang.Numbers/lt ?i ?chunkn)
        (let* [?sym (.nth ?chunk ?i)]
          (do ?body (recur ?seq ?chunk ?chunkn (clojure.lang.Numbers/unchecked_inc ?i))))
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
                  (do ?body (recur (clojure.core/next ?bs) nil 0 0)))))))))
    (foreach (m/app u/set-meta ?sym :mut true)
             ?xs
             (m/app inner-form ?body))

    ;; loop -> ???
    ;;(loop* ?bindings ?body)
    ;;(loop ?bindings (m/app inner-form ?body))
    ))

(def def-init
  (s/rewrite
    (def ?name ?form)
    (m/app as-init ?name ?form :immutable)

    (def ?name)
    (init ?name)))

(def assignments
  (s/rewrite
    ;; with-local-vars
    (let* [_ (.setDynamic (clojure.lang.Var/create)) ..?n]
      (do
        (clojure.lang.Var/pushThreadBindings (clojure.core/hash-map . !sym !x ..?n))
        (try
          ?body
          (finally (clojure.lang.Var/popThreadBindings)))))
    ;;->
    (do
      . (m/app as-init !sym !x :mutable) ..?n
      (m/app inner-form ?body))

    ;; let
    (let* [!sym !x ...]
      ?body)
    ;;->
    (do
      . (m/app as-init !sym !x :immutable) ...
      (m/app inner-form ?body))

    ;; TODO: Clojure has nuanced concepts of state...
    ;; should we boil them all down to assignment, provide equivalent abstractions, or only support 1?
    ;; currently boiling them all down to basic assignment.

    (var-get ?x)
    ?x

    (var-set ?var ?x)
    (assign ?var (m/app inner-form ?x))

    (reset! ?a ?x)
    (assign ?a (m/app inner-form ?x))

    (ref-set ?r ?x)
    (assign ?r (m/app inner-form ?x))

    (swap! ?a ?f & ?args)
    (group
      (assign ?a (invoke ?f ?a & ?args))
      ?a)

    (alter ?r ?f & ?args)
    (assign ?r (invoke ?f ?r & ?args))

    (alter-var-root ?v ?f & ?args)
    (assign ?v (invoke ?f ?v & ?args))

    (send ?a ?f & ?args)
    (assign ?a (invoke ?f ?a & ?args))

    (send-off ?a ?f & ?args)
    (assign ?a (invoke ?f ?a & ?args))

    ;; (atom (+ 1 2))
    ((m/pred ref-symbol? _) ?x)
    (m/app inner-form ?x)

    (deref ?x)
    ?x

    (clojure.core/deref ?x)
    ?x))

(def conditionals
  (s/rewrite
    (if ?test ?then)
    (if (m/app inner-form ?test) (m/app inner-form ?then))

    (if ?test ?then ?else)
    (if (m/app inner-form ?test) (m/app inner-form ?then) (m/app inner-form ?else))

    (let* [?auto ?x]
      (case* ?auto ?shift ?mask
             ?default ;;often (throw (new java.lang.IllegalArgumentException (clojure.core/str "No matching clause: " ?auto)))
             ?imap ?switch-type ?tt ?skip-check))
    (case (m/app inner-form ?x) ?imap)))

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
  "Ordered from most to least specific."
  (s/choice
    loops
    conditionals
    assignments
    def-init
    operators
    data-literals
    misc
    s/pass))

(def def-function
  (s/rewrite
    ;; defn
    (def ?name
      (fn* .
        (!params . !body-forms ..!n)
        ..?m))
    ;;->
    (arity-group . (function ?name . !params .
                             (m/app inner-form !body-forms) ..!n)
                 ..?m)))

(def top-level-form
  (s/choice
    def-function
    def-init
    ;; TODO: update docs and examples
    inner-form
    (s/rewrite ?else ~(throw (ex-info "fail" {:else ?else})))))


;; takes a sequence of forms, returns a single form
(def rewrite-namespace
  (s/rewrite
    ;; ns
    ((do (clojure.core/in-ns ('quote ?ns-name)) & _)
     . !forms ...)
    ;;->
    (namespace ?ns-name .
               (m/app top-level-form !forms)
               ...)

    ?else ~(throw (ex-info "fail" {:else ?else}))))

(def remove-groups
  "Remove groups surrounding the multiple arities of a single function"
  (s/rewrite
    (namespace ?ns-name
               .
               (m/or (arity-group . !stuff ...) !stuff) ...)
    (namespace ?ns-name
               .
               !stuff ...)))

(def rewrite (comp remove-groups rewrite-namespace))
