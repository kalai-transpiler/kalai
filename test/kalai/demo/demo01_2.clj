(ns kalai.demo.demo01-2)

(defn format ^String [^int num]
  (let [i (atom num)
        ^StringBuffer result (StringBuffer.)]
    (while (not (= @i 0))
      (let [^int quotient (quot @i 10)
            ^int remainder (rem @i 10)]
        (.insert result (int 0) remainder)
        (reset! i quotient)))
    (.toString result)))

(defn -main []
  (println (format 12345)))
