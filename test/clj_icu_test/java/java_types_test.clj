(ns clj-icu-test.java.java-types-test
  (:require [clj-icu-test.common :refer :all]
            [clj-icu-test.emit.api :refer :all]
            [clj-icu-test.emit.langs :as l]
            [clojure.tools.analyzer.jvm :as az]
            [expectations :refer :all])
  (:import clj_icu_test.common.AstOpts
           java.util.List
           java.util.Map
           java.util.Set))


(reset-indent-level)

(let [ast (az/analyze '(def ^{:type [List [Integer]]} numbers [13 17 19 23]))]
  (expect "List<Integer> numbers = Arrays.asList(13, 17, 19, 23);"
          (emit (map->AstOpts {:ast ast :lang ::l/java}))))
(let [ast (az/analyze '(do (def x 13) (def ^{:type [List [Integer]]} numbers [x])))]
  (expect ["x = 13;"
           "List<Integer> numbers = Arrays.asList(x);"]
          (emit (map->AstOpts {:ast ast :lang ::l/java})))
  )

(let [ast (az/analyze '{"one" 1
                        "two" 2
                        "three" 3})]
  )
(let [ast (az/analyze '(def ^{:type [Map [String Integer]]} number-words {"one" 1
                                                                          "two" 2
                                                                          "three" 3}))]
  )

(let [ast (az/analyze '#{"smell" "sight" "touch" "taste" "hearing"})]
  )
(let [ast (az/analyze '(def ^{:type [Set [String]]} senses #{"smell" "sight" "touch" "taste" "hearing"}))]
  )

(let [ast (az/analyze '(def ^{:type [Map [String List [Character]]]} number-systems-map {}))]
  )
