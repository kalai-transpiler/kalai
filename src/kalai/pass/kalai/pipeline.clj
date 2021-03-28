(ns kalai.pass.kalai.pipeline
  (:require [kalai.pass.kalai.a-annotate-ast :as a-annotate-ast]
            [kalai.pass.kalai.b-kalai-constructs :as b-kalai-constructs]
            [kalai.pass.kalai.c-operators :as c-operators]
            [kalai.pass.kalai.d-annotate-return :as d-annotate-return]
            [kalai.pass.kalai.f-keyword-set-map-functions :as f]
            [clojure.tools.analyzer.passes.jvm.emit-form :as e]
            [kalai.util :as u]))

(defn asts->kalai [asts]
  (->> asts
       (a-annotate-ast/rewrite)
       (map e/emit-form)
       (b-kalai-constructs/rewrite)
       (c-operators/rewrite)
       (d-annotate-return/rewrite)
       (f/rewrite)))
