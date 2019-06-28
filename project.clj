(defproject clj-icu-test "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/tools.analyzer.jvm "0.7.2"]]
  :profiles {:dev {:dependencies [[expectations "2.1.10"]]}}
  :repl-options {:init-ns clj-icu-test.core})
