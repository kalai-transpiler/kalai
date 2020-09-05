(ns kalai.pass.kalai.pipeline
  (:require [kalai.pass.kalai.a-annotate-ast :as a-annotate-ast]
            [kalai.pass.kalai.b-kalai-constructs :as b-kalai-constructs]
            [kalai.pass.kalai.d-annotate-return :as d-annotate-return]
            [clojure.tools.analyzer.passes.jvm.emit-form :as azef]))

(defn asts->kalai [asts]
  (->> asts
       (map a-annotate-ast/rewrite)
       (map azef/emit-form)
       (b-kalai-constructs/rewrite)
       (d-annotate-return/rewrite)))
