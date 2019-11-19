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
        metadata (meta annotated-form)
        analyzed-form-ast (if env
                            (az/analyze annotated-form-without-meta env)
                            (az/analyze annotated-form-without-meta))
        impl-state (:impl-state ast-opts)
        new-impl-state (merge-with merge impl-state metadata)
        analyzed-form-ast-opts (assoc ast-opts
                                      :ast analyzed-form-ast
                                      :impl-state new-impl-state)]
    analyzed-form-ast-opts))

(defn shortest-raw-form
  "Return the value whose string-ified version is shortest"
  [raw-forms]
  (let [raw-forms-by-size (sort-by (comp count str) raw-forms)
        shortest-raw-form (first raw-forms-by-size)]
    shortest-raw-form))
