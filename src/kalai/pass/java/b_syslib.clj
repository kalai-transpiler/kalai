(ns kalai.pass.java.b-syslib
  (:require [meander.strategy.epsilon :as s]))

;; If do do this before syntax, we can remove j/invoke... is that good or bad?

;; TODO: I don't believe at this level we can identify (let [println #()] (println)) is not System.out.println
;; We might have to address this at the ast level, or somehow force fully qualified Clojure core functions.

(def m
  {#'clojure.core/println
   'System.out.println})

(def c
  '{StringBuffer [StringBuffer {append append
                                length length}]})

(def rust
  '{StringBuffer [String {(.append ?x)          (push_str ?x)
                          length                length
                          (.toString ?x)        ?x
                          (.insert ?x ?idx ?s2) (r/block
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
