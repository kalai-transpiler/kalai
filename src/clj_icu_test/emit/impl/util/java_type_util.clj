(ns clj-icu-test.emit.impl.util.java-type-util
  (:require [clj-icu-test.common :refer :all]
            [clj-icu-test.emit.interface :as iface :refer :all]
            [clojure.string :as string]
            [clojure.tools.analyzer.jvm :as az]))


(defn java-emit-const-complex-type
  [ast-opts]
  {:pre [(is-complex-type? ast-opts)
         (= :vector (or (-> ast-opts :ast :type)
                        (-> ast-opts :ast :op)))]}
  (let [ast (:ast ast-opts)
        item-asts (if-not (:literal? ast)
                    (:items ast)
                    (let [item-vals (:val ast)]
                      (map az/analyze item-vals)))
        item-strs (map emit (map (partial assoc ast-opts :ast) item-asts))
        ;; TODO: figure out how to auto-import java.util.Arrays
        item-strs-comma-separated (string/join ", " item-strs)
        expr-parts ["Arrays.asList("
                    item-strs-comma-separated
                    ")"]
        expr (apply str expr-parts)]
    expr))
