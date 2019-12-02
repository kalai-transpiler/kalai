(ns clj-icu-test.demo.demo02-logic-test
  (:require [clj-icu-test.demo.demo02 :as demo :refer :all]
            [clojure.test :refer [deftest testing]]
            [expectations.clojure.test :refer :all]))

;; Run the parse and format functions on a small set of inputs for each number system
(deftest demo02-logic
  (testing "parse"
    (expect 50301 (parse "50301"))
    (expect 321 (parse "321"))
    (expect 21 (parse "21"))
    (expect 1 (parse "1"))
    (expect 50301 (parse "\u0665\u0660\u0663\u0660\u0661"))
    (expect 321 (parse "\u0663\u0662\u0661"))
    (expect 21 (parse "\u0662\u0661"))
    (expect 1 (parse "\u0661"))
    (expect 50301 (parse "৫০৩০১"))
    (expect 321 (parse "৩২১"))
    (expect 21 (parse "২১"))
    (expect 1 (parse "১")))
  (testing "format"
    (expect "50301" (format 50301 "LATIN"))
    (expect "321" (format 321 "LATIN"))
    (expect "21" (format 21 "LATIN"))
    (expect "1" (format 1 "LATIN"))
    (expect "\u0665\u0660\u0663\u0660\u0661" (format 50301 "ARABIC"))
    (expect "\u0663\u0662\u0661" (format 321 "ARABIC"))
    (expect "\u0662\u0661" (format 21 "ARABIC"))
    (expect "\u0661" (format 1 "ARABIC"))
    (expect "৫০৩০১" (format 50301 "BENGALI"))
    (expect "৩২১" (format 321 "BENGALI"))
    (expect "২১" (format 21 "BENGALI"))
    (expect "১" (format 1 "BENGALI"))))

;; Verify that the data used in the demo is correctly structured
(deftest demo02-logic-digit-maps
  (let [digits-map (getDigitsMap)
        number-systems-map (getNumberSystemsMap)
        num-sys-digit-value-pairs (for [[num-sys-name num-sys-digits] number-systems-map
                                       [idx digit] (map-indexed vector num-sys-digits)]
                                   [digit idx])
        single-level-digit-values-map (into {} num-sys-digit-value-pairs)]
    (expect digits-map single-level-digit-values-map)))
