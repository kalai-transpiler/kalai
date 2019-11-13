(ns clj-icu-test.emit.impl.util.curlybrace-util
  (:require [clojure.tools.analyzer.jvm :as az]))

(defn unwrap-with-meta
  "Given an AstOpts, unwrap the :with-meta operation at the root of the AST to
  given the form that the metadata annotates, and return as AstOpts"
  [ast-opts]
  {:pre [(= :with-meta (:op (:ast ast-opts)))]}
  (let [ast (:ast ast-opts)
        env (:env ast)
        annotated-form (:form ast)
        annotated-form-without-meta (vary-meta annotated-form select-keys [])
        analyzed-form-ast (if env
                            (az/analyze annotated-form-without-meta env)
                            (az/analyze annotated-form-without-meta))
        analyzed-form-ast-opts (assoc ast-opts :ast analyzed-form-ast)]
    analyzed-form-ast-opts))
