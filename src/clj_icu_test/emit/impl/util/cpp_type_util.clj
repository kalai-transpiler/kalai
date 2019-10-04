(ns clj-icu-test.emit.impl.util.cpp-type-util
  (:require [clj-icu-test.common :refer :all]
            [clj-icu-test.emit.impl.util.common-type-util :as common-type-util]
            [clj-icu-test.emit.interface :as iface :refer :all]
            [clojure.string :as string]
            [clojure.tools.analyzer.jvm :as az]))

(defn cpp-emit-const-vector-not-nested
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

(defn cpp-emit-assignment-vector-nested-recursive
  "Recursive implementation fn for cpp-emit-assignment-vector-nested.
  Return value is 2-element vector [sub-vector-identifier new-statements].
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
              expr (cpp-emit-const-vector-not-nested ast-opts)
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
                            (cpp-emit-assignment-vector-nested-recursive item-ast-opts new-type-class-ast identifier new-position-vector statements))))
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

(defn cpp-emit-assignment-vector-nested
  "element-type and identifier are strings"
  [ast-opts type-class-ast identifier]
  {:pre [(is-complex-type? ast-opts)
         (common-type-util/is-const-vector-nested? ast-opts)]}
  (let [init-position-vector []
        init-statements []
        result (cpp-emit-assignment-vector-nested-recursive ast-opts
                                                             type-class-ast
                                                             identifier
                                                             init-position-vector
                                                             init-statements)
        [identifier statements] result
        statements-val-opts (map->AnyValOpts (assoc ast-opts :val statements))]
    (emit-statements statements-val-opts)))

(defn cpp-emit-assignment-vector-not-nested
  [ast-opts]
  (let [ast (:ast ast-opts)
        type-class-ast (get-assignment-type-class-ast ast-opts)
        type-class-ast-opts (assoc ast-opts :ast type-class-ast)
        type-str (emit-type type-class-ast-opts)
        identifier (when-let [identifer-symbol (get-assignment-identifier-symbol ast-opts)]
                     (str identifer-symbol))
        expr-ast-opts (update-in ast-opts [:ast] :init)
        expr (cpp-emit-const-vector-not-nested expr-ast-opts) 
        statement-parts [type-str
                         identifier
                         "="
                         expr]
        statement-parts-opts (-> ast-opts
                                 (assoc :val statement-parts)
                                 map->AnyValOpts)
        statement (emit-statement statement-parts-opts)]
    statement))

(defn cpp-emit-assignment-map-not-nested
  [ast-opts]
  (let [ast (:ast ast-opts)
        type-class-ast (get-assignment-type-class-ast ast-opts)
        type-class-ast-opts (assoc ast-opts :ast type-class-ast)
        type-str (emit-type type-class-ast-opts) 
        identifier (when-let [identifer-symbol (get-assignment-identifier-symbol ast-opts)]
                     (str identifer-symbol)) 
        initialize-statement-parts [type-str
                                    identifier]
        map-form-entry-seq (-> ast
                               :init
                               :form
                               seq)
        map-entry-env (or (:env ast-opts)
                          (:env ast))
        map-entry-ast-opts (assoc ast-opts :env map-entry-env)
        map-form-entry-str-seq (for [[k-form v-form] map-form-entry-seq] 
                                 (let [k-str (emit-arg map-entry-ast-opts k-form)
                                       v-str (emit-arg map-entry-ast-opts v-form) 
                                       result [k-str v-str]]
                                   result))
        map-entry-put-statements (for [[k-str v-str :as entry] map-form-entry-str-seq]
                                   (let [put-statement-args entry
                                         put-statement-args-str (string/join ", " put-statement-args)
                                         statement-parts [(str identifier ".insert(std::make_pair(")
                                                          put-statement-args-str
                                                          "))"]
                                         statement (string/join statement-parts)]
                                     statement))
        all-statement-data-seq (concat [initialize-statement-parts]
                                       map-entry-put-statements)
        all-statement-data-seq-val-opts (map->AnyValOpts
                                         (assoc ast-opts :val all-statement-data-seq))
        all-statement-str-seq (emit-statements all-statement-data-seq-val-opts)]
    all-statement-str-seq))
