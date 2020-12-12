(ns a.demo01
  (:refer-clojure :exclude [format]))

(defn format ^String [^Integer num]
  (let [i (atom ^Integer num)
        ^StringBuffer result (StringBuffer.)]
    (while (not (= @i 0))
      (let [^int quotient (quot @i 10)
            ^int remainder (rem @i 10)]
        (.insert result (int 0) remainder)
        (reset! i quotient)))
    (.toString result)))

(defn -main ^{:t :void} [& args]
  (format 2345)
  (println (format 2345)))