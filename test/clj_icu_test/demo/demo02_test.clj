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
  public Map<Character,Integer> getDigitsMap()
  {
    {
      Map<Character,Integer> m = new HashMap<>();
      m.put('٠', 0);
      m.put('١', 1);
      m.put('٢', 2);
      m.put('٣', 3);
      m.put('٤', 4);
      m.put('٥', 5);
      m.put('০', 0);
      m.put('٦', 6);
      m.put('১', 1);
      m.put('٧', 7);
      m.put('২', 2);
      m.put('٨', 8);
      m.put('৩', 3);
      m.put('٩', 9);
      m.put('৪', 4);
      m.put('৫', 5);
      m.put('৬', 6);
      m.put('৭', 7);
      m.put('৮', 8);
      m.put('৯', 9);
      m.put('0', 0);
      m.put('1', 1);
      m.put('2', 2);
      m.put('3', 3);
      m.put('4', 4);
      m.put('5', 5);
      m.put('6', 6);
      m.put('7', 7);
      m.put('8', 8);
      m.put('9', 9);
      return m;
    }
  }

  Map<Character,Integer> digitsMap = getDigitsMap();

  public Integer parse(String s)
  {
    {
      Integer result = 0;
      Integer strLength = s.length();
      for(Integer i = 0; i < strLength; i++)
      {
        {
          Character digit = s.get(i);
          Integer digitVal = digitsMap.get(digit);
          result = (10 * (result)) + digitVal;
        }
      }
      return result;
    }
  }

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
  std::map<char16_t,int> getDigitsMap()
  {
    {
      std::map<char16_t,int> m;
      m.insert(std::make_pair('٠', 0));
      m.insert(std::make_pair('١', 1));
      m.insert(std::make_pair('٢', 2));
      m.insert(std::make_pair('٣', 3));
      m.insert(std::make_pair('٤', 4));
      m.insert(std::make_pair('٥', 5));
      m.insert(std::make_pair('০', 0));
      m.insert(std::make_pair('٦', 6));
      m.insert(std::make_pair('১', 1));
      m.insert(std::make_pair('٧', 7));
      m.insert(std::make_pair('২', 2));
      m.insert(std::make_pair('٨', 8));
      m.insert(std::make_pair('৩', 3));
      m.insert(std::make_pair('٩', 9));
      m.insert(std::make_pair('৪', 4));
      m.insert(std::make_pair('৫', 5));
      m.insert(std::make_pair('৬', 6));
      m.insert(std::make_pair('৭', 7));
      m.insert(std::make_pair('৮', 8));
      m.insert(std::make_pair('৯', 9));
      m.insert(std::make_pair('0', 0));
      m.insert(std::make_pair('1', 1));
      m.insert(std::make_pair('2', 2));
      m.insert(std::make_pair('3', 3));
      m.insert(std::make_pair('4', 4));
      m.insert(std::make_pair('5', 5));
      m.insert(std::make_pair('6', 6));
      m.insert(std::make_pair('7', 7));
      m.insert(std::make_pair('8', 8));
      m.insert(std::make_pair('9', 9));
      return m;
    }
  }

  std::map<char16_t,int> digitsMap = getDigitsMap();

  int parse(std::string s)
  {
    {
      int result = 0;
      int strLength = s.length();
      for(int i = 0; i < strLength; i++)
      {
        {
          char16_t digit = s[i];
          int digitVal = digitsMap[digit];
          result = (10 * (result)) + digitVal;
        }
      }
      return result;
    }
  }

  std::map<std::string,std::vector<char16_t>> getNumberSystemsMap()
  {
    {
      std::map<std::string,std::vector<char16_t>> m;
      m.insert(std::make_pair(\"LATIN\", {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'}));
      m.insert(std::make_pair(\"ARABIC\", {'٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩'}));
      m.insert(std::make_pair(\"BENGALI\", {'০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯'}));
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
