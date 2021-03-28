(ns kalai.demo.demo01-2
  (:refer-clojure :exclude [format]))

(defn format ^String [^long num]
  (let [i (atom num)
        ^StringBuffer result (StringBuffer.)]
    (while (not (= @i 0))
      (let [^long quotient (quot @i 10)
            ^long remainder (rem @i 10)]
        (.insert result (int 0) remainder)
        (reset! i quotient)))
    (.toString result)))

(defn -main []
  (println (format 12345)))
