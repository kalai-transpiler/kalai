(ns kalai.emit.impl.util.rust-type-util
  (:require [kalai.common :refer :all]
            [kalai.emit.impl.util.common-type-util :as common-type-util]
            [kalai.emit.impl.util.rust-util :as rust-util]
            [kalai.emit.interface :as iface :refer :all]
            [clojure.string :as string]
            [clojure.tools.analyzer.jvm :as az]))


(defn rust-emit-const-vector-not-nested
  [ast-opts]
  {:pre [(is-complex-type? ast-opts)
         (or (= java.util.List (-> ast-opts :impl-state :type-class-ast :mtype))
             (= java.util.List (-> ast-opts :impl-state :type-class-ast :mtype first))
             (= :vector (or (-> ast-opts :ast :type)
                            (-> ast-opts :ast :op))))]}
  (let [ast (:ast ast-opts)

        ;; item-form-seq (:form ast)
        ;; item-ast-opts (-> ast-opts
        ;;                   (assoc :env (-> ast-opts :ast :env))
        ;;                   (update-in [:impl-state :type-class-ast :mtype] second))
        ;; item-strs (map (partial emit-arg item-ast-opts) item-form-seq)
        
        ;; item-asts (if-not (:literal? ast)
        ;;             (:items ast)
        ;;             (let [item-vals (:val ast)]
        ;;               (map az/analyze item-vals)))
        ;; item-strs (map emit (map (partial assoc ast-opts :ast) item-asts))

        item-strs  (let [item-form-seq (:form ast) 
                          item-ast-opts (-> ast-opts
                                            (assoc :env (-> ast-opts :ast :env))
                                            (update-in [:impl-state :type-class-ast :mtype] second))]
                     (map (partial rust-util/emit-arg-val item-ast-opts) item-form-seq))
        
        
        ;; if (:literal? ast)
        ;; (let [item-vals (:val ast)
        ;;       item-asts (map az/analyze item-vals)]
        ;;   (map emit (map (partial assoc ast-opts :ast) item-asts)))
        
        
        
        ;; TODO: figure out how to auto-import java.util.Arrays
        item-strs-comma-separated (string/join ", " item-strs)
        expr-parts ["vec!["
                    item-strs-comma-separated
                    "]"]
        expr (apply str expr-parts)]
    expr))

(defn rust-emit-assignment-vector-nested-recursive
  "Recursive implementation fn for rust-emit-assignment-vector-nested.
  Return value is 2-element vector [sub-vector-identifier new-statements].
  Only works for nested Lists currently.
  Supports nested collections (at least N-dimension types of a single type),
  but may not yet support all configurations of nested parameters."
  [ast-opts]
  {:pre [(= :const (-> ast-opts :ast :op))
         (= :vector (-> ast-opts :ast :type))
         (-> ast-opts :impl-state :type-class-ast)
         (-> ast-opts :impl-state :identifier)
         (-> ast-opts :impl-state :position-vector)
         (-> ast-opts :impl-state :statements)
         (= java.util.List (-> ast-opts :impl-state :type-class-ast :mtype first))]}
  (let [ast (:ast ast-opts)
        impl-state (:impl-state ast-opts)
        {:keys [type-class-ast identifier position-vector statements]} impl-state
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
          ;; TODO: consolidate/refactor calls to analyzer of args and literals
        (let [

              ;; item-asts (map #(az/analyze % (:env ast-opts)) item-form-seq)
              ;; item-ast-opts-seq (map #(assoc ast-opts :ast %) item-asts)
              ;; item-strs (map emit item-ast-opts-seq)
              
              ;; item-ast-opts (-> ast-opts
              ;;                   (assoc :env (-> ast-opts :ast :env))
              ;;                   (update-in [:impl-state :type-class-ast :mtype] second))
              ;; item-strs (map (partial emit-arg item-ast-opts) item-form-seq)

              
              
              expr (rust-emit-const-vector-not-nested ast-opts)
              is-atom (rust-util/is-assignment-expr-atom (:ast ast-opts))
              is-immutable (not is-atom)
              identifier-and-type (if is-immutable
                                    (if type-str
                                      [(str "let " sub-vector-identifier ":") type-str]
                                      [(str "let " sub-vector-identifier)])
                                    (if type-str
                                      [(str "let mut " sub-vector-identifier ":") type-str]
                                      [(str "let mut " sub-vector-identifier)]))
              statement-parts (concat
                               identifier-and-type
                               ["="
                                expr])
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
                                new-position-vector (conj position-vector idx)
                                new-impl-state (assoc impl-state
                                                      :type-class-ast new-type-class-ast
                                                      :position-vector new-position-vector)
                                new-item-ast-opts (assoc item-ast-opts :impl-state new-impl-state)]
                            (emit-assignment-complex-type new-item-ast-opts))))
            collected-statements (->> item-strs
                                      (map #(if (seqable? %) (second %) %))
                                      ;;(apply concat)
                                      flatten
                                      )            
            new-vector-items (->> item-strs
                                  (map #(if (seqable? %) (first %) %)))
            expr (->> new-vector-items
                      (string/join ", ")
                      (#(str "vec![" % "]")))
            is-atom (rust-util/is-assignment-expr-atom (:ast ast-opts))
            is-immutable (not is-atom)
            identifier-and-type (if is-immutable
                                  (if type-str
                                    [(str "let " identifier ":") type-str]
                                    [(str "let " identifier)])
                                  (if type-str
                                    [(str "let mut " identifier ":") type-str]
                                    [(str "let mut " identifier)]))
            statement-parts (concat
                             identifier-and-type
                             ["="
                              expr])            
            statement-parts-opts (-> ast-opts
                                     (assoc :val statement-parts)
                                     map->AnyValOpts)
            statement (emit-statement statement-parts-opts)
            new-statements (concat collected-statements
                                   [statement])
            return-val [sub-vector-identifier new-statements]]
        return-val))))

(defn rust-emit-assignment-vector-nested
  "element-type and identifier are strings"
  [ast-opts]
  {:pre [(is-complex-type? ast-opts)
         (-> ast-opts :impl-state :type-class-ast)
         (-> ast-opts :impl-state :identifier)
         (common-type-util/is-const-vector-nested? ast-opts)]}
  (let [impl-state (:impl-state ast-opts)
        {:keys [type-class-ast identifier position-vector statements]} impl-state
        init-position-vector (or position-vector [])
        init-statements (or statements [])
        ast-opts-init-impl-state (update-in ast-opts [:impl-state] merge {:type-class-ast type-class-ast
                                                                          :identifier identifier
                                                                          :position-vector init-position-vector
                                                                          :statements init-statements})
        result (rust-emit-assignment-vector-nested-recursive ast-opts-init-impl-state)
        [identifier statements] result
        statements-val-opts (map->AnyValOpts (assoc ast-opts :val statements))]
    (emit-statements statements-val-opts)))

(defn rust-emit-assignment-vector-not-nested
  [ast-opts]
  (let [ast (:ast ast-opts)
        type-class-ast (get-assignment-type-class-ast ast-opts)
        type-class-ast-opts (assoc ast-opts :ast type-class-ast)
        type-str (emit-type type-class-ast-opts)
        identifier (if-let [impl-state-identifier (-> ast-opts :impl-state :identifier)]
                     (let [position-vector (-> ast-opts :impl-state :position-vector)]
                        (->> position-vector
                             (map #(str "V" %))
                             (apply str)
                             (str impl-state-identifier)))
                     (or (-> ast-opts :impl-state :identifier)
                         (when-let [identifer-symbol (get-assignment-identifier-symbol ast-opts)]
                           (str identifer-symbol))))        
        expr-ast (or (:init ast)
                     ast)
        expr-ast-opts (assoc ast-opts :ast expr-ast)
        expr (rust-emit-const-vector-not-nested expr-ast-opts)
        is-atom (rust-util/is-assignment-expr-atom expr-ast)
        is-immutable (not is-atom)
        identifier-and-type (if is-immutable
                              (if type-str
                                [(str "let " identifier ":") type-str]
                                [(str "let " identifier)])
                              (if type-str
                                [(str "let mut " identifier ":") type-str]
                                [(str "let mut " identifier)]))
        statement-parts (concat
                         identifier-and-type
                         ["="
                          expr])
        statement-parts-opts (-> ast-opts
                                 (assoc :val statement-parts)
                                 map->AnyValOpts)
        statement (emit-statement statement-parts-opts)
        return-val (if (-> ast-opts :impl-state :identifier)
                     [identifier statement]
                     statement)]
    return-val))

(defn rust-emit-assignment-map-not-nested
  [ast-opts]
  (let [ast (:ast ast-opts)
        type-class-ast (get-assignment-type-class-ast ast-opts)
        type-class-ast-opts (assoc ast-opts :ast type-class-ast)
        type-str (emit-type type-class-ast-opts) 
        identifier (when-let [identifer-symbol (get-assignment-identifier-symbol ast-opts)]
                     (str identifer-symbol))
        identifier-and-type (if type-str
                              [(str "let mut " identifier ":") type-str]
                              [(str "let mut " identifier)])
        initialize-statement-parts (concat
                                    identifier-and-type
                                    ["="
                                     "HashMap::new()"])
        map-form-entry-seq (-> ast
                               :init
                               :form
                               seq)
        map-entry-env (or (:env ast-opts)
                          (:env ast))
        map-entry-ast-opts (assoc ast-opts :env map-entry-env)
        map-form-entry-str-seq (for [[k-form v-form] map-form-entry-seq] 
                                 (let [k-str (rust-util/emit-arg-val map-entry-ast-opts k-form) ;; use emit-arg-val b/c Rust collections expect value arguments
                                       v-str (rust-util/emit-arg-val map-entry-ast-opts v-form) 
                                       result [k-str v-str]]
                                   result))
        map-entry-put-statements (for [[k-str v-str :as entry] map-form-entry-str-seq]
                                   (let [put-statement-args entry
                                         put-statement-args-str (string/join ", " put-statement-args)
                                         statement-parts [(str identifier ".insert(")
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

(defn rust-emit-assignment-map-nested-recursive
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
                (str sub-map-identifier ".insert(" key-str ", " val-str ")"))]
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
              type-str (str "HashMap<" map-key-type-str "," map-val-type-str ">")
              identifier-and-type (if type-str
                              [(str "let mut " identifier ":") type-str]
                              [(str "let mut " identifier)])
              initialize-statement-parts (concat
                                          identifier-and-type
                                          ["="
                                           "HashMap::new()"])
              map-put-statements (map map-put-statement key-strs val-strs)
              all-statement-data-seq (concat [initialize-statement-parts]
                                             map-put-statements)
              all-statement-data-seq-val-opts (map->AnyValOpts
                                               (assoc ast-opts :val all-statement-data-seq))
              all-statement-str (emit-statements all-statement-data-seq-val-opts)
              all-statement-str-seq [all-statement-str]
              return-val [sub-map-identifier all-statement-str-seq]]
          return-val)))))

(defn rust-emit-assignment-map-nested
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
        result (rust-emit-assignment-map-nested-recursive ast-opts-init-impl-state)
        [identifier statements] result
        statements-val-opts (map->AnyValOpts (assoc ast-opts :val statements))] 
    (emit-statements statements-val-opts)))
