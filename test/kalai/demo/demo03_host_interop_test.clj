(ns kalai.demo.demo03-host-interop-test
  (:require [kalai.common :refer :all]
            [kalai.emit.api :refer :all]
            [kalai.emit.langs :as l]
            [kalai.emit.util :as emit-util]
            [kalai.placation]
            [clojure.test :refer [deftest testing]]
            [clojure.tools.analyzer.jvm :as az]
            [clojure.pprint :refer :all]
            [expectations.clojure.test :refer :all]))

(deftest demo03
  (let [ast-seq (az/analyze-ns `kalai.demo.demo03-host-interop)]
    (testing "java"
      (let [java-strs (emit-util/emit-analyzed-ns-asts ast-seq ::l/java)
            ast-opts-seq (->> ast-seq
                              (map #(assoc {} :ast % :lang ::l/java))
                              (map map->AstOpts))]
        (expect [""
"public class HostInterop
{
  public String printEnvVariables()
  {
    String envEditor = System.getenv(\"EDITOR\");
    return envEditor;
  }
}
"] java-strs)))))