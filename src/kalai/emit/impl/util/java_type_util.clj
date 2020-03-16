(ns kalai.emit.impl.util.java-type-util
  (:require [kalai.common :refer :all]
            [kalai.emit.impl.util.common-type-util :as common-type-util]
            [kalai.emit.interface :as iface :refer :all]
            [clojure.string :as string]
            [clojure.tools.analyzer.jvm :as az]))

(defn java-emit-const-complex-type
  [ast-opts]
  {:pre [(is-complex-type? ast-opts)
         (= :vector (or (-> ast-opts :ast :type)
                        (-> ast-opts :ast :op)))]}
  (let [ast (:ast ast-opts)

        
        ;; item-asts (if-not (:literal? ast)
        ;;             (:items ast)
        ;;             (let [item-vals (:val ast)]
        ;;               (map az/analyze item-vals)))
        ;; item-strs (map emit (map (partial assoc ast-opts :ast) item-asts))


        item-strs  (let [item-form-seq (:form ast)
                          item-ast-opts (-> ast-opts
                                            (assoc :env (-> ast-opts :ast :env))
                                            (update-in [:impl-state :type-class-ast :mtype] second))]
                     (map (partial emit-arg item-ast-opts) item-form-seq))
        
        ;; TODO: figure out how to auto-import java.util.Arrays
        item-strs-comma-separated (string/join ", " item-strs)
        expr-parts ["Arrays.asList("
                    item-strs-comma-separated
                    ")"]
        expr (apply str expr-parts)]
    expr))

(defn java-emit-assignment-map-not-nested
  [ast-opts]
  (let [ast (:ast ast-opts)
        type-class-ast (get-assignment-type-class-ast ast-opts)
        type-class-ast-opts (assoc ast-opts :ast type-class-ast)
        type-str (emit-type type-class-ast-opts) 
        identifier (when-let [identifer-symbol (get-assignment-identifier-symbol ast-opts)]
                     (str identifer-symbol))
        initialize-statement-rhs "new HashMap<>()"
        initialize-statement-parts [type-str
                                    identifier
                                    "="
                                    initialize-statement-rhs]
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
                                         statement-parts [(str identifier ".put(")
                                                          put-statement-args-str
                                                          ")"]
                                         statement (string/join statement-parts)]
                                     statement))
        all-statement-data-seq (concat [initialize-statement-parts]
                                       map-entry-put-statements) 
        all-statement-data-seq-val-opts (map->AnyValOpts
                                         (assoc ast-opts :val all-statement-data-seq))
        all-statement-str (emit-statements all-statement-data-seq-val-opts)] 
    all-statement-str))

(defn java-emit-assignment-map-nested-recursive
  [ast-opts]
  {:pre [(or (and (= :const (-> ast-opts :ast :op))
                  (= :map (-> ast-opts :ast :type))) 
             (= :map (-> ast-opts :ast :op)))
         (or (= java.util.Map (-> ast-opts :impl-state :type-class-ast :mtype first))
             (= java.util.Map (-> ast-opts :impl-state :type-class-ast first)))
         (-> ast-opts :impl-state :type-class-ast)
         (-> ast-opts :impl-state :identifier)
         (-> ast-opts :impl-state :position-vector)
         (-> ast-opts :impl-state :statements)]}
  (letfn [(analyze-literal [literal]
            (if-let [env (or (-> ast-opts :env)
                             (-> ast-opts :ast :env))]
              (az/analyze literal env)
              (az/analyze literal)))]
    (let [ast (:ast ast-opts)
          impl-state (:impl-state ast-opts)
          {:keys [type-class-ast identifier position-vector statements]} impl-state
          key-asts (or (-> ast :keys)
                       (let [key-literals (-> ast :form keys)
                             key-literal-asts (map analyze-literal key-literals)]
                         key-literal-asts))
          val-asts (or (-> ast :vals)
                       (let [val-literals (-> ast :form vals)
                             val-literal-asts (map analyze-literal val-literals)]
                         val-literal-asts))
          key-ast-opts (-> ast-opts
                           (assoc :env (-> ast-opts :ast :env))
                           (update-in [:impl-state :type-class-ast :mtype] (comp first second)))
          val-ast-opts (-> ast-opts
                           (assoc :env (-> ast-opts :ast :env))
                           (update-in [:impl-state :type-class-ast :mtype] (comp second second)))
          key-ast-opts-seq (map #(assoc key-ast-opts :ast %) key-asts)
          val-ast-opts-seq (map #(assoc val-ast-opts :ast %) val-asts)
          key-strs (map emit key-ast-opts-seq)
          val-strs (map emit val-ast-opts-seq)
          sub-map-identifier (->> position-vector
                                  (map #(str "M" %))
                                  (apply str)
                                  (str identifier))]
      ;; for now, going to assume that all map keys are not the kind that return a seq of strings
      (assert (not (some common-type-util/val-with-nesting? key-strs)))
      (letfn [(map-put-statement [key-str val-str]
                (str sub-map-identifier ".put(" key-str ", " val-str ")"))]
        (let [map-key-type-ast (update-in type-class-ast [:mtype] (comp first second))
              map-val-type-ast (update-in type-class-ast [:mtype] (comp second second)) 
              map-key-type-ast-opts (-> ast-opts
                                        (assoc :ast map-key-type-ast)
                                        (assoc-in [:impl-state :type-class-ast] map-key-type-ast))
              map-val-type-ast-opts (-> ast-opts
                                        (assoc :ast map-val-type-ast)
                                        (assoc-in [:impl-state :type-class-ast] map-val-type-ast))
              map-key-type-str (emit-type map-key-type-ast-opts)
              map-val-type-str (emit-type map-val-type-ast-opts)
              type-str (str "Map<" map-key-type-str "," map-val-type-str ">")
              initialize-statement-rhs "new HashMap<>()"
              initialize-statement-parts [type-str
                                          identifier
                                          "="
                                          initialize-statement-rhs]
              map-put-statements (map map-put-statement key-strs val-strs)
              all-statement-data-seq (concat [initialize-statement-parts]
                                             map-put-statements)
              all-statement-data-seq-val-opts (map->AnyValOpts
                                               (assoc ast-opts :val all-statement-data-seq))
              all-statement-str (emit-statements all-statement-data-seq-val-opts)
              all-statement-str-seq [all-statement-str]
              return-val [sub-map-identifier all-statement-str-seq]]
          return-val)))))

(defn java-emit-assignment-map-nested
  [ast-opts]
  {:pre [(or (and (= :const (-> ast-opts :ast :op))
                  (= :map (-> ast-opts :ast :type))) 
             (= :map (-> ast-opts :ast :op)))
         (is-complex-type? ast-opts)
         (-> ast-opts :impl-state :type-class-ast)
         (-> ast-opts :impl-state :identifier)]}
  (let [ast (:ast ast-opts)
        impl-state (:impl-state ast-opts)
        {:keys [type-class-ast identifier]} impl-state
        init-position-vector []
        init-statements []
        ast-opts-init-impl-state (update-in ast-opts [:impl-state] merge {:type-class-ast type-class-ast
                                                                          :identifier identifier
                                                                          :position-vector init-position-vector
                                                                          :statements init-statements})
        result (java-emit-assignment-map-nested-recursive ast-opts-init-impl-state)
        [identifier statements] result
        statements-val-opts (map->AnyValOpts (assoc ast-opts :val statements))] 
    (emit-statements statements-val-opts)))
