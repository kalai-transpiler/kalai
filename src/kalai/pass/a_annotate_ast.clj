(ns kalai.pass.a-annotate-ast
  (:require [meander.strategy.epsilon :as s]
            [meander.epsilon :as m]
            [clojure.walk :as walk]))

(defn trim-ast
  "When you don't want to look at the entire thing"
  [ast]
  (walk/postwalk #(if (map? %)
                    (dissoc % :column :line :file :env)
                    %)
                 ast))

(def type-aliases
  ;; TODO: bottom-up is too slow, as it steps through EVERYTHING, not just the ast nodes
  (s/bottom-up
    (s/rewrite
      ;; replace type aliases with their definition
      ;; TODO: while this mostly works, sometimes it breaks, and we should do a more specific walk anyhow (only traverse maps)
      #_#_
      {:name (m/and (m/pred some? ?name)
                    (m/app meta {:t (m/pred symbol? ?t) & ?name-meta}))
       :meta (m/and (m/pred some? ?meta)
                    {:keys [!as ..?n {:val :t} . !bs ..?m]
                     :vals [!cs ..?n {:var (m/app #(:kalias (meta %)) ?kalias)} . !ds ..?m]})
       &     ?ast}
      ;;->
      {:name ~(with-meta ?name (assoc ?name-meta :t ?kalias))
       :meta ?meta
       &     ?ast}

      ;; erase type aliases from the AST
      [!before ..?n
       {:op   :def
        :meta {:val {:kalias (m/pred (complement nil?))}}}
       . !after ..?m]
      ;;->
      [!before ..?n !after ..?m]

      ;; otherwise leave the ast as is
      ?else ?else)))

(def rewrite
  "There is contextual information in the AST that is not available in s-expressions.
  The purpose of this pass is to capture that information and modify the s-expressions to contain what we need."
  type-aliases)
