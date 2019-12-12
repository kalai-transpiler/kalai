(ns clj-icu-test.demo.demo02-logic-test
  (:require [clj-icu-test.demo.demo02 :as demo :refer :all]
            [clojure.test :refer [deftest testing]]
            [expectations.clojure.test :refer :all]))

(deftest demo02-logic-separator-positions
  (testing "NONE"
    (expect [] (getSeparatorPositions 4 "NONE"))
    (expect [] (getSeparatorPositions 6 "NONE"))
    (expect [] (getSeparatorPositions 7 "NONE")))
  (testing "ON_ALIGNED_3_3"
    (expect [1] (getSeparatorPositions 4 "ON_ALIGNED_3_3"))
    (expect [3] (getSeparatorPositions 6 "ON_ALIGNED_3_3"))
    (expect [4 1] (getSeparatorPositions 7 "ON_ALIGNED_3_3")))
  (testing "ON_ALIGNED_3_2"
    (expect [1] (getSeparatorPositions 4 "ON_ALIGNED_3_2"))
    (expect [3 1] (getSeparatorPositions 6 "ON_ALIGNED_3_2"))
    (expect [4 2] (getSeparatorPositions 7 "ON_ALIGNED_3_2")))
  (testing "MIN_2"
    (expect [] (getSeparatorPositions 4 "MIN_2"))
    (expect [3] (getSeparatorPositions 6 "MIN_2"))
    (expect [4 1] (getSeparatorPositions 7 "MIN_2"))))

;; Run the parse and format functions on a small set of inputs for each number system

(deftest demo02-logic-parse
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
  (testing "parse - grouping separators"
    (expect 7654321 (parse "7,654,321"))
    (expect 7654321 (parse "76,54,321"))
    (expect 4321 (parse "4,321"))
    (expect 4321 (parse "4321"))))

(deftest demo02-logic-format
   (testing "format"
     (expect "50301" (format 50301 "LATIN" "NONE"))
     (expect "321"   (format 321 "LATIN" "NONE"))
     (expect "21"    (format 21 "LATIN" "NONE"))
     (expect "1"     (format 1 "LATIN" "NONE"))
     (expect "\u0665\u0660\u0663\u0660\u0661" (format 50301 "ARABIC" "NONE"))
     (expect "\u0663\u0662\u0661"             (format 321 "ARABIC" "NONE"))
     (expect "\u0662\u0661"                   (format 21 "ARABIC" "NONE"))
     (expect "\u0661"                         (format 1 "ARABIC" "NONE"))
     (expect "৫০৩০১" (format 50301 "BENGALI" "NONE"))
     (expect "৩২১"   (format 321 "BENGALI" "NONE"))
     (expect "২১"    (format 21 "BENGALI" "NONE"))
     (expect "১"     (format 1 "BENGALI" "NONE")))
  (testing "format - grouping separators"
    (testing "ON_ALIGNED_3_3"
      (testing "Latin digits"
        (expect "1"         (format 1 "LATIN" "ON_ALIGNED_3_3"))
        (expect "21"        (format 21 "LATIN" "ON_ALIGNED_3_3"))
        (expect "321"       (format 321 "LATIN" "ON_ALIGNED_3_3"))
        (expect "4,321"     (format 4321 "LATIN" "ON_ALIGNED_3_3"))
        (expect "54,321"    (format 54321 "LATIN" "ON_ALIGNED_3_3"))
        (expect "654,321"   (format 654321 "LATIN" "ON_ALIGNED_3_3"))
        (expect "7,654,321" (format 7654321 "LATIN" "ON_ALIGNED_3_3")))
      (testing "Bengali digits (separator is comma)"
        (expect "১"         (format 1 "BENGALI" "ON_ALIGNED_3_3"))
        (expect "২১"        (format 21 "BENGALI" "ON_ALIGNED_3_3"))
        (expect "৩২১"       (format 321 "BENGALI" "ON_ALIGNED_3_3"))
        (expect "৪,৩২১"     (format 4321 "BENGALI" "ON_ALIGNED_3_3"))
        (expect "৫৪,৩২১"    (format 54321 "BENGALI" "ON_ALIGNED_3_3"))
        (expect "৬৫৪,৩২১"   (format 654321 "BENGALI" "ON_ALIGNED_3_3"))
        (expect "৭,৬৫৪,৩২১" (format 7654321 "BENGALI" "ON_ALIGNED_3_3")))
      (testing "Arabic digits & Arabic separator"
        (expect "١"         (format 1 "ARABIC" "ON_ALIGNED_3_3"))
        (expect "٢١"        (format 21 "ARABIC" "ON_ALIGNED_3_3"))
        (expect "٣٢١"       (format 321 "ARABIC" "ON_ALIGNED_3_3"))
        (expect "٤٬٣٢١"     (format 4321 "ARABIC" "ON_ALIGNED_3_3"))
        (expect "٥٤٬٣٢١"    (format 54321 "ARABIC" "ON_ALIGNED_3_3"))
        (expect "٦٥٤٬٣٢١"   (format 654321 "ARABIC" "ON_ALIGNED_3_3"))
        (expect "٧٬٦٥٤٬٣٢١" (format 7654321 "ARABIC" "ON_ALIGNED_3_3"))))
    (testing "ON_ALIGNED_3_2"
      (testing "Latin digits"
        (expect "1"         (format 1 "LATIN" "ON_ALIGNED_3_2"))
        (expect "21"        (format 21 "LATIN" "ON_ALIGNED_3_2"))
        (expect "321"       (format 321 "LATIN" "ON_ALIGNED_3_2"))
        (expect "4,321"     (format 4321 "LATIN" "ON_ALIGNED_3_2"))
        (expect "54,321"    (format 54321 "LATIN" "ON_ALIGNED_3_2"))
        (expect "6,54,321"  (format 654321 "LATIN" "ON_ALIGNED_3_2"))
        (expect "76,54,321" (format 7654321 "LATIN" "ON_ALIGNED_3_2"))))))

;; Verify that the data used in the demo is correctly structured
(deftest demo02-logic-digit-maps
  (let [digits-map (getDigitsMap)
        number-systems-map (getNumberSystemsMap)
        num-sys-digit-value-pairs (for [[num-sys-name num-sys-digits] number-systems-map
                                       [idx digit] (map-indexed vector num-sys-digits)]
                                   [digit idx])
        single-level-digit-values-map (into {} num-sys-digit-value-pairs)]
    (expect digits-map single-level-digit-values-map)))
