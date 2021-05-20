(ns kalai.pass.rust.pipeline
  (:require [kalai.pass.rust.a-syntax :as rust.a-syntax]
            [kalai.pass.rust.ab-cast :as rust.ab-cast]
            [kalai.pass.rust.b-function-call :as rust.b-function-call]
            [kalai.pass.rust.c-condense :as rust.c-condense]
            [kalai.pass.rust.e-string :as rust.e-string]
            [kalai.pass.shared.flatten-groups :as flatten-groups]
            [kalai.util :as u]))

(defn kalai->rust [k]
  (->> k
       (flatten-groups/rewrite)
       (rust.a-syntax/rewrite)
       (rust.ab-cast/rewrite)
       (rust.b-function-call/rewrite)
       (rust.c-condense/rewrite)
       (rust.e-string/stringify-entry)))
