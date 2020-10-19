(ns kalai.pass.java.pipeline
  (:require [kalai.pass.java.a-syntax :as java1-syntax]
            [kalai.pass.java.b-function-call :as java2-syslib]
            [kalai.pass.java.c-condense :as java3-condense]
            [kalai.pass.java.e-string :as java4-string]
            [kalai.pass.shared.flatten-groups :as flatten-groups]
            [kalai.pass.shared.raise-stuff :as raise-stuff]
            [kalai.util :as u]))

(defn kalai->java [k]
  (->> k
       (flatten-groups/rewrite)
       (java1-syntax/rewrite)
       (raise-stuff/rewrite)
       (flatten-groups/rewrite)
       (java2-syslib/rewrite)
       (java3-condense/rewrite)
       (java4-string/stringify-entry)))
