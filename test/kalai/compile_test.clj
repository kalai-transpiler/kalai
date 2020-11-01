(ns kalai.compile-test
  (:require [clojure.test :refer :all]
            [kalai.emit.langs :as l]
            [kalai.compile :as c]
            [kalai.util :as u]))

;; for testing one file
(deftest compile-source-file-test
  #_
  (let [s (c/compile-source-file
            "examples/src/main/clj/b/type_alias.clj")]
    (and
      (is s)
      (is (nil? (c/write-target-file
                  s
                  "b/typeAlias.clj"
                  "examples/src/main/java"
                  ::l/java)))
      (is (nil? (c/compile-target-file
                  "examples/src/main/java/b/typeAlias.java"
                  ::l/java
                  "examples/tmp"))))))

;; tests all files in examples
(deftest compile-test
  (reset! u/c 0)
  (let [x {:in       "examples/src/main/clj"
           :out      "examples/src/main/java"
           :target   "examples/tmp"
           :language ::l/java}]
    (is (nil? (c/compile x)))
    (is (nil? (c/target-compile x)))))
