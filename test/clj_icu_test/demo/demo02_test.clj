(ns clj-icu-test.demo.demo02-test
  (:require [clj-icu-test.common :refer :all]
            [clj-icu-test.emit.api :refer :all]
            [clj-icu-test.emit.langs :as l]
            [clj-icu-test.emit.util :as emit-util] 
            [clojure.tools.analyzer.jvm :as az]
            [expectations.clojure.test :refer :all])
  (:import clj_icu_test.common.AstOpts))




;; (defexpect demo02-as-unit-tests
;;   (require '[clj-icu-test.common :refer :all])
;;   (import '[java.util List Map])
;;   (let [ast (az/analyze '(defclass "NumFmt"
                           
;;                            (def ^{:mtype [Map [String [List [Character]]]]}
;;                              numberSystemsMap {"LATIN" [\0 \1 \9]})
                           
;;                            (defn format ^String [^Integer num]
;;                              (let [^Integer i (atom num)
;;                                    ^StringBuffer result (atom (new-strbuf))]
;;                                (while (not (= @i 0))
;;                                  (let [^Integer quotient (quot @i 10)
;;                                        ^Integer remainder (rem @i 10)]
;;                                    (reset! result (prepend-strbuf @result remainder))
;;                                    (reset! i quotient)))
;;                                (return (tostring-strbuf @result))))))]
;;     (expect (emit (map->AstOpts {:ast ast :lang ::l/java}))
;; "public class NumFmt
;; {
;;   Map<String,List<Character>> numberSystemsMap = new HashMap<>();
;;   numberSystemsMap.put(\"LATIN\", Arrays.asList('0', '1', '9'));

;;   public String format(Integer num)
;;   {
;;     {
;;       Integer i = num;
;;       StringBuffer result = new StringBuffer();
;;       while (!((i) == 0))
;;       {
;;         {
;;           Integer quotient = (i) / 10;
;;           Integer remainder = (i) % 10;
;;           result = result.insert(0, remainder);
;;           i = quotient;
;;         }
;;       }
;;       return result.toString();
;;     }
;;   }
;; }")))








;; ast-seq is now a sequence of ASTs representing all the code in the
;; namespace ("file") clj-icu-test.demo.demo

(let [ast-seq (az/analyze-ns 'clj-icu-test.demo.demo02)]

  (defexpect demo02-java
    (let [java-strs (emit-util/emit-analyzed-ns-asts ast-seq ::l/java)]
      (expect [""
"public class NumFmt
{
  public Map<String,List<Character>> getNumberSystemsMap()
  {
    {
      Map<String,List<Character>> m = new HashMap<>();
      m.put(\"LATIN\", Arrays.asList('0', '1', '9'));
      return m;
    }
  }

  Map<String,List<Character>> numberSystemsMap = getNumberSystemsMap();

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
  
  )





;; (deftest demo02
;;   (require '[clojure.test :refer [deftest testing]])
;;   (let [ast-seq (az/analyze-ns 'clj-icu-test.demo.demo02)]

;;     ;; ast-seq is now a sequence of ASTs representing all the code in the
;;     ;; namespace ("file") clj-icu-test.demo.demo
    

;;     (testing "java"
;;       (let [java-strs (emit-util/emit-analyzed-ns-asts ast-seq ::l/java)]
;;         (expect [""
;; "public class NumFmt
;; {
;;   public String format(Integer num)
;;   {
;;     {
;;       Integer i = num;
;;       StringBuffer result = new StringBuffer();
;;       while (!((i) == 0))
;;       {
;;         {
;;           Integer quotient = (i) / 10;
;;           Integer remainder = (i) % 10;
;;           result = result.insert(0, remainder);
;;           i = quotient;
;;         }
;;       }
;;       return result.toString();
;;     }
;;   }
;; }"]
;;                 java-strs)))

    
;;     (testing "cpp"
;;       (let [cpp-strs (emit-util/emit-analyzed-ns-asts ast-seq ::l/cpp)]
;;         (expect [""
;; "class NumFmt
;; {
;;   std::string format(int num)
;;   {
;;     {
;;       int i = num;
;;       std::string result = \"\";
;;       while (!((i) == 0))
;;       {
;;         {
;;           int quotient = (i) / 10;
;;           int remainder = (i) % 10;
;;           result = std::to_string(remainder) + result;
;;           i = quotient;
;;         }
;;       }
;;       return result;
;;     }
;;   }
;; };"]
;;                 cpp-strs)))))
