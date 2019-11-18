(ns clj-icu-test.demo.demo02-test
  (:require [clj-icu-test.common :refer :all]
            [clj-icu-test.emit.api :refer :all]
            [clj-icu-test.emit.langs :as l]
            [clj-icu-test.emit.util :as emit-util]
            [clojure.test :refer [deftest testing]]
            [clojure.tools.analyzer.jvm :as az]
            [expectations.clojure.test :refer :all])
  (:import clj_icu_test.common.AstOpts))


(deftest demo02
  ;; ast-seq is now a sequence of ASTs representing all the code in the
  ;; namespace ("file") clj-icu-test.demo.demo

  (let [ast-seq (az/analyze-ns 'clj-icu-test.demo.demo02)]

    (testing "java"
      (let [java-strs (emit-util/emit-analyzed-ns-asts ast-seq ::l/java)]
        (expect [""
                 "public class NumFmt
{
  public Map<String,List<Character>> getNumberSystemsMap()
  {
    {
      Map<String,List<Character>> m = new HashMap<>();
      m.put(\"LATIN\", Arrays.asList('0', '1', '2', '3', '4', '5', '6', '7', '8', '9'));
      m.put(\"ARABIC\", Arrays.asList('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩'));
      m.put(\"BENGALI\", Arrays.asList('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯'));
      return m;
    }
  }

  Map<String,List<Character>> numberSystemsMap = getNumberSystemsMap();

  public String format(Integer num, String numberSystem)
  {
    {
      Integer i = num;
      StringBuffer result = new StringBuffer();
      while (!((i) == 0))
      {
        {
          Integer quotient = (i) / 10;
          Integer remainder = (i) % 10;
          List<Character> numberSystemDigits = numberSystemsMap.get(numberSystem);
          Character localDigit = numberSystemDigits.get(remainder);
          result = result.insert(0, localDigit);
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
  std::map<std::string,std::vector<char16_t>> getNumberSystemsMap()
  {
    {
      std::map<std::string,std::vector<char16_t>> m;
      m.put(\"LATIN\", {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'});
      m.put(\"ARABIC\", {'٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩'});
      m.put(\"BENGALI\", {'০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯'});
      return m;
    }
  }

  std::map<std::string,std::vector<char16_t>> numberSystemsMap = getNumberSystemsMap();

  std::string format(int num, std::string numberSystem)
  {
    {
      int i = num;
      std::string result = \"\";
      while (!((i) == 0))
      {
        {
          int quotient = (i) / 10;
          int remainder = (i) % 10;
          std::vector<char16_t> numberSystemDigits = numberSystemsMap[numberSystem];
          char16_t localDigit = numberSystemDigits[remainder];
          result = localDigit + result;
          i = quotient;
        }
      }
      return result;
    }
  }
};"]
                cpp-strs)))
    )
  )
