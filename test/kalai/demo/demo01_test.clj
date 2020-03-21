(ns kalai.demo.demo01-test
  (:require [kalai.emit.langs :as l]
            [kalai.emit.util :as emit-util]
            [clojure.test :refer [deftest testing]]
            [clojure.tools.analyzer.jvm :as az]
            [expectations.clojure.test :refer :all]))

(deftest demo01
  (let [ast-seq (az/analyze-ns 'kalai.demo.demo01)]

    ;; ast-seq is now a sequence of ASTs representing all the code in the
    ;; namespace ("file") kalai.demo.demo
    

    (testing "java"
      (let [java-strs (emit-util/emit-analyzed-ns-asts ast-seq ::l/java)]
        (expect [""
"public class NumFmt
{
  public String format(Integer num)
  {
    {
      Integer i = num;
      StringBuffer result = new StringBuffer();
      while (!((i) == 0))
      {
        {
          Integer quotient = (i) / 10;
          Integer remainder = (i) % 10;
          result = result.insert(0, remainder);
          i = quotient;
        }
      }
      return result.toString();
    }
  }
}"]
                java-strs)))

    
    (testing "cpp"
      (let [cpp-strs (emit-util/emit-analyzed-ns-asts ast-seq ::l/cpp)]
        (expect [""
"class NumFmt
{
  std::string format(int num)
  {
    {
      int i = num;
      std::string result = \"\";
      while (!((i) == 0))
      {
        {
          int quotient = (i) / 10;
          int remainder = (i) % 10;
          result = std::to_string(remainder) + result;
          i = quotient;
        }
      }
      return result;
    }
  }
};"]
                cpp-strs)))))
