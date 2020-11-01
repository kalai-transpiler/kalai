(ns kalai.pass.kalai.b-kalai-constructs
  (:require [meander.strategy.epsilon :as s]
            [meander.epsilon :as m]
            [kalai.util :as u]
            [clojure.string :as str]))

(declare inner-form)

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

(def ref-vars
  #{#'atom
    #'ref
    #'agent})

(def swap-vars
  #{#'swap!
    #'alter
    #'alter-var-root
    #'send
    #'send-off})

(def reset-vars
  #{#'reset!
    #'var-set
    #'ref-set})

(defn ref-form? [x]
  (and (seq? x)
       (ref-vars (:var (meta (first x))))))

(defn as-init
  [?name ?x mutability]
  (list 'init
        (u/maybe-meta-assoc ?name :mut
                            (or (#{:mutable} mutability)
                        (ref-form? ?x)))
        (inner-form (u/propagate-type ?name ?x))))

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
      (init ~(with-meta ?sym {:t   (or (:t (meta ?sym)) 'int)
                              :mut true})
            0)
      (while (operator < ?sym ?n)
        . (m/app inner-form !body) ...
        (assign ?sym (operator ++ ?sym))))

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
    (foreach (m/app u/maybe-meta-assoc ?sym :mut true)
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

    ((u/var ~#'var-get) ?x)
    ?x

    ;; matches (reset! atom value)
    ;; and var-set, ref-set
    ((u/var (m/pred reset-vars)) ?ref ?x)
    (assign ?ref ~(inner-form (u/propagate-type ?ref ?x)))

    ;; matches (swap! atom fn arg1 arg2)
    ;; and send, send-off, alter, alter-var-root
    ;; for non-mutable variables which cannot be assigned to
    ((u/var (m/pred swap-vars))
     (m/pred #(not (:mut (meta %))) ?ref)
     ?f . !args ...)
    (invoke (m/app u/propagate-type ?ref ?f) ?ref . (m/app inner-form !args) ...)

    ;; matches (swap! atom fn arg1 arg2)
    ;; and send, send-off, alter, alter-var-root
    ;; for mutable variables that can be assigned to
    ((u/var (m/pred swap-vars)) ?ref ?f . !args ...)
    (group
      (assign ?ref
              (invoke (m/app u/propagate-type ?ref ?f)
                      ?ref
                      . (m/app inner-form !args) ...))
      ?ref)

    ;; matches (atom (+ 1 2)) or (ref (+ 1 2)) or (agent (+ 1 2))
    (m/and
      ((u/var (m/pred ref-vars)) ?x . _ ...)
      ?ref)
    ~(inner-form (u/propagate-type ?ref ?x))

    ((u/var ~#'deref) ?ref)
    ?ref

    ;; TODO: might be better to leave this to function-calls
    ;; TODO: sets use 'contains', array lists use count
    ((u/var ~#'contains?) ?coll ?x)
    (method containsKey ?coll ?x)))

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


    ;; constructor
    (new ?c . !args ...)
    (new ?c . (m/app inner-form !args) ...)


    ;; method

    ('. ?obj ?method . !args ...)
    (method ?method (m/app inner-form ?obj)
            . (m/app inner-form !args) ...)

    (m/and
      ('.. ?obj . (!methods . !args ..!n) ...)
      (m/let [?tmp (u/tmp-for ?obj)]))
    (group
      (init ?tmp (m/app inner-form ?obj))
      . (!methods ?tmp . (m/app inner-form !args) ..!n) ...)

    ((m/and (m/pred #(str/starts-with? (str %) "."))
            (m/app #(symbol (subs (str %) 1)) ?m))
     ?obj . !args ...)
    (method ?m (m/app inner-form ?obj)
            . (m/app inner-form !args) ...)

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
    (s/rewrite ?else ~(throw (ex-info "Top level form" {:else ?else})))))

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

    ?else ~(throw (ex-info "Namespace" {:else ?else}))))

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
