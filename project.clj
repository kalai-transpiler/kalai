(defproject kalai "0.1.0-SNAPSHOT"
  :description "Kalai transpiler is a source-to-source transpiler to convert Clojure to multiple target languages (Rust, C++, Java, ...)"
  :url "https://github.com/echeran/kalai"
  :license {:name "EPL-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :main kalai.core
  :dependencies [[expectations "2.1.10"]
                 [org.clojure/clojure "1.10.1"]
                 [org.clojure/tools.analyzer.jvm "1.1.0"]
                 [org.clojure/tools.cli "1.0.194"]
                 [meander/epsilon "0.0.480"]])
