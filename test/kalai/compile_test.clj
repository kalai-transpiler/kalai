(ns kalai.compile-test
  (:require [clojure.test :refer :all]
            [kalai.emit.langs :as l]
            [kalai.exec.kalai-to-language :as k]
            [kalai.exec.language-compilers :as lc]
            [kalai.util :as u]
            [clojure.java.io :as io]))

(deftest compile-source-file-test
  (reset! u/c 0)
  (let [transpiled-file (io/file "examples/java/src/b/TypeAlias.java")]
    (if (.exists transpiled-file)
      (.delete transpiled-file))
    (k/transpile-file (io/file "examples/src/main/clj/b/type_alias.clj")
                      {:src-dir "examples/src/main/clj"
                       :transpile-dir "examples"
                       :languages     #{::l/java}})
    (is (.exists transpiled-file))
    ;; We're leaving this single-file compilation test because it is possible
    ;; in our current Java impl to compile a single file in isolation (no proj
    ;; structure & build file required).
    (is (nil? (lc/compile-target-file transpiled-file
                                      ::l/java
                                      "examples/tmp")))))

(deftest compile-rust-source-file-test
  (reset! u/c 0)
  (let [transpiled-file (io/file "examples/rust/src/b/type_alias.rs")]
    (if (.exists transpiled-file)
      (.delete transpiled-file))
    (k/transpile-file (io/file "examples/src/main/clj/b/type_alias.clj")
                      {:src-dir "examples/src/main/clj"
                       :transpile-dir "examples"
                       :languages     #{::l/rust}})
    (is (.exists transpiled-file))
    ;; Because we depend on a crate by default, we would need to use `cargo`,
    ;; not `rustc`
    #_(is (nil? (lc/compile-target-file transpiled-file
                                      ::l/rust
                                      "examples/tmp")))))

(deftest compile-java-test
  (reset! u/c 0)
  (let [x {:src-dir       "examples/src/main/clj"
           :transpile-dir "examples"
           :languages     #{::l/java}}]
    (is (= "" (with-out-str (k/transpile-all x))))
    (is (= nil (lc/build #{::l/java} "examples")))))

(deftest compile-rust-test
  (reset! u/c 0)
  (let [x {:src-dir       "examples/src/main/clj"
           :transpile-dir "examples"
           :language      #{::l/rust}}]
    (is (= "" (with-out-str (k/transpile-all x))))
    (is (= nil (lc/build #{::l/rust} "examples")))))
