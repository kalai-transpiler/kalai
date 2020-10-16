(ns kalai.pass.kalai.a-annotate-ast
  (:require [meander.strategy.epsilon :as s]
            [meander.epsilon :as m]
            [clojure.tools.analyzer.ast :as ast]
            [kalai.util :as u]))

(def substitute-aliased-types
  (s/rewrite
    ;; replace type aliases with their definition
    {:name (m/and (m/pred some? ?name)
                  (m/app meta {:as ?name-meta
                               :t  (m/pred symbol? ?t)}))
     :meta {:as   ?meta
            :keys [!as ..?n {:val :t} . !bs ..?m]
            :vals [!cs ..?n
                   {:var (m/app meta {:kalias (m/pred some? ?kalias)})}
                   . !ds ..?m]}
     &     ?ast}
    ;;->
    {:name ~(with-meta ?name (assoc ?name-meta :t ?kalias))
     :meta ?meta
     &     ?ast}

    ;; annotate vars with their var as metadata so they can be identified later in the pipeline
    {:op   :var
     :var  (m/pred some? ?var)
     :form (m/pred some? ?form)
     &     ?ast}
    ;;->
    {:op   :var
     :var  ?var
     :form (m/app u/set-meta ?form :var ?var)
     &     ?ast}

    ;; otherwise leave the ast as is
    ?else
    ?else))

(def erase-type-alias
  (s/rewrite
    {:op   :def
     :meta {:val {:kalias (m/pred some?)}}}
    ;;->
    nil

    ?else
    ?else))

(defn rewrite
  "There is contextual information in the AST that is not available in s-expressions.
  The purpose of this pass is to capture that information and modify the s-expressions to contain what we need."
  [ast]
  (-> ast
      (ast/prewalk substitute-aliased-types)
      erase-type-alias))
