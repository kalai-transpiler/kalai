(ns kalai.pass.java.pipeline
  (:require [kalai.pass.java.a-syntax :as java.a-syntax]
            [kalai.pass.java.ab-cast :as java.ab-cast]
            [kalai.pass.java.b-function-call :as java.b-function-call]
            [kalai.pass.java.c-condense :as java.c-condense]
            [kalai.pass.java.e-string :as java.e-string]
            [kalai.pass.shared.flatten-groups :as flatten-groups]
            [kalai.pass.shared.raise-stuff :as raise-stuff]
            [kalai.util :as u]))

(defn kalai->java [k]
  (->> k
       (flatten-groups/rewrite)
       (java.a-syntax/rewrite)
       (java.ab-cast/rewrite)
       (raise-stuff/rewrite)
       (flatten-groups/rewrite)
       (java.b-function-call/rewrite)
       (java.c-condense/rewrite)
       (java.e-string/stringify-entry)))
