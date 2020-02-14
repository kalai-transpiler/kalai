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

(defn ast-has-nil-value?
  "Return whether the AST comes from input that evaluates to nil"
  [ast]
  (and (= :const (-> ast :op))
       (= :nil (-> ast :type))))

(defn is-ast-nil?
  "Return whether the AST from the analyzer appears to come from nil as the input"
  [ast]
  (and (ast-has-nil-value? ast)
       (not (-> ast :raw-forms))))

(defn is-ast-false?
  "Return whether the AST from the analyzer appears to come from false as the input"
  [ast]
  (and (= :const (-> ast :op))
       (= :bool (-> ast :type))
       (= false (-> ast :val))))

(defn is-terminal-cond-form
  "Return whether we have reached a terminal value when traversing the if-else expansion from a cond form"
  [ast]
  (and (ast-has-nil-value? ast)
       (not (is-ast-nil? ast))))

(defn- cond-ast-test-expr-pairs-recursive
  [ast pairs]
  (if (is-terminal-cond-form ast)
    pairs
    (let [test-ast (:test ast)
          then-ast (:then ast)
          new-pair [test-ast then-ast]
          new-pairs (conj pairs new-pair)
          else-ast (:else ast)
          new-ast else-ast]
      (recur new-ast new-pairs))))

(defn cond-ast-test-expr-pairs
  [ast]
  (let [curr-pairs []]
    (cond-ast-test-expr-pairs-recursive ast curr-pairs)))

(defn new-name
  "Provide a new name (ex: name for a new identifier) that is based on the input name such that it is guaranteed to be unique.
  This will use gensym provide a random output (a unique number appended to the input).
  During testing, shadow this function with a pseudo-random fn or constant value fn to test against predictable, non-random output."
  [name]
  (str (gensym name)))
