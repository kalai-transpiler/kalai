(ns kalai.compile-test
  (:require [clojure.test :refer :all]
            [kalai.emit.langs :as l]
            [kalai.compile :as c]
            [kalai.util :as u]
            [clojure.string :as string]))

;; for testing one file
(deftest compile-source-file-test
  #_(let [s (c/transpile-source-file-content
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
    (is (= "" (with-out-str (c/transpile x))))
    ;; TODO: consider making compile order matter when running compilation with a root file
    (is (= nil (c/target-compile x)))))
