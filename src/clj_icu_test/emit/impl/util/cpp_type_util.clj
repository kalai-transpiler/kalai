(ns clj-icu-test.emit.impl.util.cpp-type-util
  (:require [clj-icu-test.common :refer :all]
            [clj-icu-test.emit.interface :as iface :refer :all]
            [clojure.string :as string]
            [clojure.tools.analyzer.jvm :as az]))

(defn is-const-complex-type-nested?
  [ast-opts]
  (let [ast (:ast ast-opts)
        expr-form (:form ast)]
    (assert (seqable? expr-form))
    (boolean
     (some seqable? expr-form))))

(defn cpp-emit-const-complex-type-not-nested
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
        expr-parts ["{"
                    item-strs-comma-separated
                    "}"]
        expr (apply str expr-parts)]
    expr))

(defn cpp-emit-const-complex-type-nested-recursive
  "Return value is 2-element vector [sub-vector-identifier new-statements].
  Only works for nested Lists currently.
  Supports nested collections (at least N-dimension types of a single type),
  but may not yet support all configurations of nested parameters."
  [ast-opts type-class-ast identifier position-vector statements]
  {:pre [(= :const (-> ast-opts :ast :op))
         (= :vector (-> ast-opts :ast :type))
         (= java.util.List (-> type-class-ast :mtype first))]}
  (let [ast (:ast ast-opts)
        form (:form ast)
        item-form-seq form
        is-nested-vector (some seqable? item-form-seq)
        sub-vector-identifier (->> position-vector
                                   (map #(str "V" %))
                                   (apply str)
                                   (str identifier))
        type-class-ast-opts (assoc ast-opts :ast type-class-ast)
        type-str (emit-type type-class-ast-opts)]
    (if-not is-nested-vector
      (do
        (assert (and (= 1 (-> type-class-ast :mtype second count))
                     (not (-> type-class-ast :mtype second first seqable?))))
        (let [item-asts (map #(az/analyze % (:env ast-opts)) item-form-seq)
              item-ast-opts-seq (map #(assoc ast-opts :ast %) item-asts)
              item-strs (map emit item-ast-opts-seq) 
              expr (cpp-emit-const-complex-type-not-nested ast-opts)
              statement-parts [type-str
                               sub-vector-identifier
                               "="
                               expr]
              statement-parts-opts (-> ast-opts
                                       (assoc :val statement-parts)
                                       map->AnyValOpts)
              statement (emit-statement statement-parts-opts)
              new-statements (concat statements [statement])
              return-val [sub-vector-identifier new-statements]]
          return-val))
      (let [re-analyze-fn (if (:env ast-opts)
                            #(az/analyze % (:env ast-opts))
                            az/analyze)
            item-asts (map re-analyze-fn item-form-seq)
            item-ast-opts-seq (map #(assoc ast-opts :ast %) item-asts)
            indexed-item-ast-opts-seq (map-indexed #(vector %1 %2) item-ast-opts-seq)
            item-strs (for [[idx item-ast-opts] indexed-item-ast-opts-seq]
                        (if-not (is-complex-type? item-ast-opts)
                          (emit item-ast-opts)
                          (let [new-type-class-ast (update-in type-class-ast [:mtype] second)
                                new-position-vector (conj position-vector idx)]
                            (cpp-emit-const-complex-type-nested-recursive item-ast-opts new-type-class-ast identifier new-position-vector statements))))
            collected-statements (->> item-strs
                                      (map #(if (seqable? %) (second %) %))
                                      (apply concat))
            new-vector-items (->> item-strs
                                  (map #(if (seqable? %) (first %) %)))
            expr (->> new-vector-items
                      (string/join ", ")
                      (#(str "{" % "}")))
            statement-parts [type-str
                             sub-vector-identifier
                             "="
                             expr]
            statement-parts-opts (-> ast-opts
                                     (assoc :val statement-parts)
                                     map->AnyValOpts)
            statement (emit-statement statement-parts-opts)
            new-statements (concat collected-statements
                                   [statement])
            return-val [sub-vector-identifier new-statements]]
        return-val))))

(defn cpp-emit-const-complex-type-nested
  "element-type and identifier are strings"
  [ast-opts type-class-ast identifier]
  {:pre [(is-complex-type? ast-opts)
         (is-const-complex-type-nested? ast-opts)]}
  (let [result (cpp-emit-const-complex-type-nested-recursive ast-opts
                                                             type-class-ast
                                                             identifier
                                                             []
                                                             [])
        [identifier statements] result]
    (string/join \newline statements)))
