(ns kalai.pass.kalai.pipeline
  (:require [kalai.pass.kalai.a-annotate-ast :as a-annotate-ast]
            [kalai.pass.kalai.b-kalai-constructs :as b-kalai-constructs]
            [kalai.pass.kalai.d-annotate-return :as d-annotate-return]
            [kalai.pass.kalai.f-keyword-set-map-functions :as f]
            [clojure.tools.analyzer.passes.jvm.emit-form :as azef]))

(defn asts->kalai [asts]
  (->> asts
       (map a-annotate-ast/rewrite)
       (remove nil?)
       (map azef/emit-form)
       (b-kalai-constructs/rewrite)
       (d-annotate-return/rewrite)
       (f/rewrite)))
