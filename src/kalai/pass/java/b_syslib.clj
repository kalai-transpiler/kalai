(ns kalai.pass.java.b-syslib
  (:require [meander.strategy.epsilon :as s]))

;; If do do this before syntax, we can remove j/invoke... is that good or bad?

;; TODO: I don't believe at this level we can identify (let [println #()] (println)) is not System.out.println
;; We might have to address this at the ast level, or somehow force fully qualified Clojure core functions.

(def m
  {#'clojure.core/println
   'System.out.println})

;; Note: For Kalai code to execute as Clojure in a REPL,
;; interop needs to be backed by Java classes,
;; therefore any new abstract logical operation that we want to support across target languages
;; must have a Java implementation.
;; For that reason forcing a Java class to exist as the key in the map makes that requirement explicit
;; while being an alternative to a more heavyweight version of defining interfaces and polymorphic dispatch
;; (for example multi-methods)
;; It doesn't allow us to share repetitive transpiled support in the way that multi-methods do.

(def c
  '{StringBuffer [StringBuffer {append append
                                length length}]})

(def rust
  '{StringBuffer [String {(new)                   (String::new)
                          (.append ^Character ?x) (.push_str ?x)
                          (.length)               (.length)
                          (.toString ?x)          ?x
                          (.insert ?x ?idx ?s2)   (r/block
                                                    (m/let [t (gensym "tmp")]
                                                           (r/assign t ?x)
                                                           (r/invoke truncate t ?idx)
                                                           (r/invoke push_str t ?s2)))}]})

(def rewrite
  (s/bottom-up
    (s/rewrite
      ;; should be clojure.core/println
      (j/invoke println & ?more)
      (j/invoke System.out.println & ?more)

      ?else ?else)))
