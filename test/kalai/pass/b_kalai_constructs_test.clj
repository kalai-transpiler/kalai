(ns kalai.pass.b-kalai-constructs-test
  (:require [clojure.test :refer :all])
  (:require [kalai.pass.b-kalai-constructs :as b]
            [clojure.tools.analyzer.jvm :as az]
            [clojure.tools.analyzer.passes.jvm.emit-form :as e]))

(deftest testit
  (is (= '(do (assignment x 1)
              (assignment y 2)
              (operator + x y))
         (b/assignments
           (e/emit-form (az/analyze '(with-local-vars [x 1, y 2] (+ (var-get x) (var-get y)))))))))
