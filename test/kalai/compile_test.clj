(ns kalai.compile-test
  (:require [clojure.test :refer :all]
            [kalai.emit.langs :as l]
            [kalai.compile :as compile]
            [kalai.util :as u]))

(deftest compile-source-file-test
  (is (compile/compile-source-file "examples/src/main/clj/a/demo01.clj")))

(deftest compile-source-file-test2
  (is (compile/compile-source-file "examples/src/main/clj/b/simple.clj")))

(deftest compile-test
  (reset! u/c 0)
  (let [x {:in       "examples/src/main/clj"
           :out      "examples/src/main/java"
           :target   "examples/tmp"
           :language ::l/java}]
    (is (nil? (compile/compile x)))
    (is (nil? (compile/target-compile x)))))
