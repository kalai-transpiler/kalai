(ns kalai.compile-test
  (:require [clojure.test :refer :all]
            [kalai.emit.langs :as l]
            [kalai.compile :as compile]))

(deftest compile-file-test
  (is (nil? (compile/compile-file "examples/a/demo01.clj" "out" true ::l/rust))))

(deftest compile-test
  (is (nil? (compile/compile {:in "./examples/a"
                              :out "out"
                              :language ::l/rust}))))

(deftest compile-source-file-test
  (is (compile/compile-source-file "examples/a/demo01.clj")))

(deftest compile-test2
  (is (nil? (compile/compile {:in "./examples/b"
                              :out "out"
                              :language ::l/java}))))
