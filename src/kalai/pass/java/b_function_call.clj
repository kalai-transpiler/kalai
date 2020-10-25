(ns kalai.pass.java.b-function-call
  (:require [kalai.util :as u]
            [meander.strategy.epsilon :as s]
            [meander.epsilon :as m]))

;; TODO: user extension point, is dynamic var good?
;; can it be more data driven?
(def ^:dynamic *user*)

;; If we do this before syntax, we can remove j/invoke... is that good or bad?

;; Note: For Kalai code to execute as Clojure in a REPL,
;; interop needs to be backed by Java classes,
;; therefore any new abstract logical operation that we want to support across target languages
;; must have a Java implementation.
;; For that reason forcing a Java class to exist as the key in the map makes that requirement explicit
;; while being an alternative to a more heavyweight version of defining interfaces and polymorphic dispatch
;; (for example multi-methods)
;; It doesn't allow us to share repetitive transpiled support in the way that multi-methods do.

(defn nth-for [x]
  ;; TODO: we need to have the type info to figure out what to do
  'charAt)

(def rewrite
  (s/bottom-up
    (s/rewrite
      (j/invoke (u/var ~#'println) & ?more)
      (j/invoke System.out.println & ?more)

      (j/invoke clojure.lang.RT/count ?x)
      (j/method length ?x)

      ;; TODO: need to do different stuff depending on the type
      (j/invoke clojure.lang.RT/nth ?x ?n)
      (j/method (m/app nth-for ?x) ?x ?n)

      (j/invoke clojure.lang.RT/get ?x ?k)
      (j/method get ?x ?k)

      ?else
      ?else)))
