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
            ^{:doc "doc-str"} ;; equality doesn't consider metadata, but trust me this is here!
            f
            (fn*
              ([x]
               (clojure.lang.Numbers/inc
                 x))))
         (kn/normalize' (aj/analyze '(defn f "doc-str" [x] (inc x))))))
  (is (= '(fn*
           ([p1__17844#]
            (clojure.lang.Numbers/inc
              p1__17844#)))
         (kn/normalize' (aj/analyze '#(inc %)))))
  (is (= '(fn*
            f
            ([x]
             (clojure.lang.Numbers/inc
               x)))
         (kn/normalize' (aj/analyze '(fn f [x] (inc x)))))))

(deftest experiment2-test
  (is (= '(function f "doc-str" [x] (clojure.lang.Numbers/inc x))
         (kn/language-concepts-sexp
           (kn/normalize' (aj/analyze '(defn f "doc-str" [x] (inc x)))))))
  (is (= '(lambda [x] (clojure.lang.Numbers/inc x))
         (kn/language-concepts-sexp
           (kn/normalize' (aj/analyze '(fn [x] (inc x))))))))

(deftest a2-test
  (is (= 1
         (aj/analyze '(defn f "doc-str" [x] (inc x))))))
(comment
  ;; maybe try rebel
  (i/inspect (aj/analyze '(defn f "doc-str" [x] (inc x))))
  (pprint/pprint (aj/analyze '(defn f "doc-str" [x] (inc x))))
  (pprint/pprint (aj/analyze '(fn f [x] (inc x)))))
