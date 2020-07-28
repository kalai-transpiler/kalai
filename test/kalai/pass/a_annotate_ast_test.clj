(ns kalai.pass.a-annotate-ast-test
  (:require [clojure.test :refer :all]
            [kalai.pass.a-annotate-ast :as a]
            [clojure.tools.analyzer.jvm :as az]
            [clojure.tools.analyzer.passes.jvm.emit-form :as e]))

(deftest ast-annotation-test
  (let [ast (az/analyze
              '(do (def ^{:kalias '[kmap [klong kstring]]} T)
                   (def ^{:t T} x)))]
    (is (= '(do (def ^{:t [kmap [klong kstring]]} x))
           (e/emit-form (a/rewrite ast))))))
