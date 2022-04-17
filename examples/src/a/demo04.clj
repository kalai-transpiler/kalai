(ns a.demo04
  (:refer-clojure :exclude [format]))

(def ^{:kalias {:mmap [:char :int]}} DigitsMap)
(def ^{:kalias {:mmap [:string {:mvector [:char]}]}} NumberSystemsMap)
(def ^{:kalias {:mvector [:char]}} NumberSystemDigits)
(def ^{:kalias {:mmap [:string :char]}} GroupingSeparatorsMap)
(def ^{:kalias {:mvector [:int]}} SeparatorPositions)

(defn getDigitsMap ^{:t DigitsMap} []
  ^{:t DigitsMap}
  {\0     (int 0)
   \1     (int 1)
   \2     (int 2)
   \3     (int 3)
   \4     (int 4)
   \5     (int 5)
   \6     (int 6)
   \7     (int 7)
   \8     (int 8)
   \9     (int 9)
   \u0660 (int 0)
   \u0661 (int 1)
   \u0662 (int 2)
   \u0663 (int 3)
   \u0664 (int 4)
   \u0665 (int 5)
   \u0666 (int 6)
   \u0667 (int 7)
   \u0668 (int 8)
   \u0669 (int 9)
   \u09E6 (int 0)
   \u09E7 (int 1)
   \u09E8 (int 2)
   \u09E9 (int 3)
   \u09EA (int 4)
   \u09EB (int 5)
   \u09EC (int 6)
   \u09ED (int 7)
   \u09EE (int 8)
   \u09EF (int 9)})

(def ^{:t DigitsMap} digitsMap (getDigitsMap))

(defn parse ^Integer [^String s]
  (let [result (atom (int 0))
        ^{:t :int} strLength (count s)]
    (dotimes [^{:t :int} i strLength]
      (let [^{:t :char} digit (nth s i)]
        (if (contains? digitsMap digit)
          (let [^Integer digitVal (get digitsMap digit)]
            (reset! result (+ (* (int 10) @result) digitVal))))))
    @result))

(defn getNumberSystemsMap
  ^{:t NumberSystemsMap} []
  (let [^{:t NumberSystemsMap} m
        {"LATIN"   [\0 \1 \2 \3 \4 \5 \6 \7 \8 \9]
         "ARABIC"  [\u0660 \u0661 \u0662 \u0663 \u0664 \u0665 \u0666 \u0667 \u0668 \u0669]
         "BENGALI" [\u09E6 \u09E7 \u09E8 \u09E9 \u09EA \u09EB \u09EC \u09ED \u09EE \u09EF]}]
    m))

(def ^{:t NumberSystemsMap}
  numberSystemsMap (getNumberSystemsMap))

(defn getGroupingSeparatorsMap
  ^{:t GroupingSeparatorsMap} []
  ^{:t GroupingSeparatorsMap}
  {"LATIN"   \,
   "ARABIC"  \٬
   "BENGALI" \,})

(def ^{:t GroupingSeparatorsMap}
  groupingSeparatorsMap (getGroupingSeparatorsMap))

(defn getSeparatorPositions ^{:t SeparatorPositions}
  [^Integer numLength ^String groupingStrategy]
  (let [^{:t SeparatorPositions} result (atom [])]
    (cond
      (= groupingStrategy "NONE")
      @result

      (= groupingStrategy "ON_ALIGNED_3_3")
      (let [^{:t :int} i (atom (- numLength (int 3)))]
        (while (< (int 0) @i)
          (swap! result conj @i)
          (reset! i (- @i (int 3))))
        @result)

      (= groupingStrategy "ON_ALIGNED_3_2")
      (let [^{:t :int} i (atom (- numLength (int 3)))]
        (while (< (int 0) @i)
          (swap! result conj @i)
          (reset! i (- @i (int 2))))
        @result)

      (= groupingStrategy "MIN_2")
      (if (<= numLength (int 4))
        @result
        (let [^{:t :int} i (atom (- numLength (int 3)))]
          (while (< (int 0) @i)
            (swap! result conj @i)
            (reset! i (- @i (int 3))))
          @result))

      :else
      @result)))

(defn format
  ^String [^Integer num, ^String numberSystem, ^String groupingStrategy]
  (let [^{:t :int} i (atom num)
        ^:mut ^StringBuffer result (StringBuffer.)]
    (while (not (= @i (int 0)))
      (let [^Integer quotient (quot @i (int 10))
            ^Integer remainder (rem @i (int 10))
            ^{:t NumberSystemDigits} numberSystemDigits (get numberSystemsMap numberSystem)
            ^{:t :char} localDigit (nth numberSystemDigits remainder)]
        (.insert result (int 0) localDigit)
        (reset! i quotient)))
    (let [^{:t :char} sep (get groupingSeparatorsMap numberSystem)
          ^{:t :int} numLength (.length result)
          ^{:t SeparatorPositions} separatorPositions (getSeparatorPositions numLength groupingStrategy)
          ^{:t :int} numPositions (count separatorPositions)]
      (dotimes [^{:t :int} idx numPositions]
        (let [^int position (nth separatorPositions idx)]
          (.insert result position sep))))
    (.toString result)))

(defn -main ^{:t :void} [& _args]
  (println (parse "\u0665\u0660\u0663\u0660\u0661"))
  (println (parse "৫০৩০১"))
  (println (parse "7,654,321"))
  (println (parse "76,54,321"))

  (println (format (int 7654321) "LATIN" "ON_ALIGNED_3_2"))
  (println (format (int 7654321) "ARABIC" "ON_ALIGNED_3_3"))
  (println (format (int 7654321) "BENGALI" "ON_ALIGNED_3_3")))
