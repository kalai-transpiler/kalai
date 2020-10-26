(ns kalai.pass.kalai.a-annotate-ast-test
  (:require [clojure.test :refer [deftest testing is]]
            [kalai.pass.kalai.a-annotate-ast :as a]
            [clojure.tools.analyzer.jvm :as az]
            [clojure.tools.analyzer.passes.jvm.emit-form :as azef]))

(deftest t-test
  (let [in '(let [x 1
                  y x]
              (println y))
        ast (az/analyze in)
        ast2 (a/rewrite ast)
        out2 (azef/emit-form ast2)
        nav #(-> % (nth 1) (nth 3) (meta) (:local))]
    (is (= 1 (nav out2)))))
