(ns kalai.normalize-test
  (:require [clojure.test :refer :all]
            [kalai.normalize :as kn]
            [clojure.tools.analyzer.jvm :as aj]
            [clojure.inspector :as i]
            [clojure.pprint :as pprint]))

(deftest normalize-test
  ;;(kn/normalize)
  )

(deftest experiment-test
  (is (= '(def
            f
            (fn*
              ([x]
               (clojure.lang.Numbers/inc
                 x))))
         (kn/normalize' (aj/analyze '(defn f "doc-str" [x] (inc x)))))))

(deftest a2-test
  (is (= 1
         (aj/analyze '(defn f "doc-str" [x] (inc x))))))
(comment
  (i/inspect (aj/analyze '(defn f "doc-str" [x] (inc x))))
  (pprint/pprint (aj/analyze '(defn f "doc-str" [x] (inc x))))
  (pprint/pprint (aj/analyze '(fn f [x] (inc x)))))
