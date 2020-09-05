(ns kalai.pass.java.pipeline
  (:require [kalai.pass.java.a-syntax :as java1-syntax]
            [kalai.pass.java.b-syslib :as java2-syslib]
            [kalai.pass.java.c-condense :as java3-condense]
            [kalai.pass.java.d-string :as java4-string]
            [kalai.pass.shared.flatten-groups :as flatten-groups]))

(defn kalai->java [k]
  (->> k
       (flatten-groups/rewrite)
       (java1-syntax/rewrite)
       (java2-syslib/rewrite)
       (java3-condense/rewrite)
       (java4-string/stringify-entry)))
