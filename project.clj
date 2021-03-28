(defproject com.github.echeran/kalai "0.1.0-SNAPSHOT"
  :description "Kalai transpiler is a source-to-source transpiler to convert Clojure to multiple target languages (Rust, C++, Java, ...)"
  :url "https://github.com/echeran/kalai"
  :license {:name "EPL-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :main kalai.exec.main
  ;; default stack size produces a stackoverflow expection when compiling some meander expressions,
  ;; so making it explicitly larger
  :jvm-opts ["-Xss2m"]
  :test-paths ["test" "examples/src/main/clj"]
  :source-paths ["src"]
  ;;:java-source-paths ["examples/src/main/java"]
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/tools.analyzer.jvm "1.1.0"]
                 [org.clojure/tools.cli "1.0.194"]
                 [camel-snake-kebab "0.4.2"]
                 [meander/epsilon "0.0.512"]
                 [mvxcvi/puget "1.3.1"]])
