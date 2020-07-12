(ns kalai.pass.d2-java-syslib
  (:require [meander.strategy.epsilon :as s]))

;; If do do this before syntax, we can remove j/invoke... is that good or bad?

;; TODO: I don't believe at this level we can identify (let [println #()] (println)) is not System.out.println
;; We might have to address this at the ast level, or somehow force fully qualified Clojure core functions.

(def rewrite
  (s/bottom-up
    (s/rewrite
      (j/invoke println & ?more)
      (j/invoke System.out.println & ?more)

      ?else ?else)))
