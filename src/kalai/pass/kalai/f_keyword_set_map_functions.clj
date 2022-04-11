(ns kalai.pass.kalai.f-keyword-set-map-functions
  (:require [meander.strategy.epsilon :as s]
            [meander.epsilon :as m]
            [kalai.util :as u]))

(def rewrite
  (s/bottom-up
    (s/rewrite
      ;; This rewrites any type aliases
      ;; TODO: this namespace name kind of hides this rule away...
      ;; TODO: Can we remove our AST kalias shenanigans and just rely on this?
      (m/and ?x (m/app meta {:t (m/app meta {:var (m/app meta {:kalias (m/pred some? ?t)})})}))
      ~(u/maybe-meta-assoc ?x :t ?t)

      (invoke (m/pred keyword? ?k) ?x)
      (invoke clojure.lang.RT/get ?x ?k)

      (invoke (m/pred keyword? ?k) ?x ?default)
      (if (contains? ?x ?k)
        (invoke clojure.lang.RT/get ?x ?k)
        ?default)

      ?else ?else)))
