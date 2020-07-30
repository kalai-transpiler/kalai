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
  (s/bottom-up
    (s/rewrite
      ;; replace type aliases with their definition
      {:name (m/and ?name (m/app meta {:t (m/pred symbol? ?t) & ?name-meta}))
       :meta {:keys [!as ..?n {:val :t} . !bs ..?m]
              :vals [!cs ..?n {:var (m/app #(:kalias (meta %)) ?kalias)} . !ds ..?m]
              :as   ?meta}
       &     ?ast}
      ;;->
      {:name ~(with-meta ?name (assoc ?name-meta :t ?kalias))
       :meta ?meta
       &     ?ast}

      ;; erase type aliases from the AST
      [!before ..?n
       {:op   :def
        :meta {:val {:kalias _}}}
       . !after ..?m]
      ;;->
      [!before ..?n !after ..?m]

      ;; otherwise leave the ast as is
      ?else ?else)))

(def rewrite
  "There is contextual information in the AST that is not available in s-expressions.
  The purpose of this pass is to capture that information and modify the s-expressions to contain what we need."
  type-aliases)
