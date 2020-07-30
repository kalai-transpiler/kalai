(ns kalai.pass.b-kalai-constructs-test
  (:require [clojure.test :refer :all])
  (:require [kalai.pass.b-kalai-constructs :as b]
            [clojure.tools.analyzer.jvm :as az]
            [clojure.tools.analyzer.passes.jvm.emit-form :as e]))

(deftest assignments-test
  (is (= '(do (init x 1)
              (init y 2)
              (operator + x y))
         (-> '(with-local-vars [x 1
                                y 2]
                (+ (var-get x) (var-get y)))
             (az/analyze)
             (e/emit-form)
             (b/assignments)))))

(deftest assignments-test-with-atoms
  (is (= '(do (init x 1)
              (init y 2)
              (do
                (assign x 3)
                (operator + x y)))
         (-> '(let [x (atom 1)
                    y (atom 2)]
                (reset! x 3)
                (+ @x (deref y)))
             (az/analyze)
             (e/emit-form)
             (b/assignments)))))
