(ns a.demo01
  (:refer-clojure :exclude [format]))

(defn format ^String [^Integer num]
  (let [i (atom ^Integer num)
        ^:mut ^StringBuffer result (StringBuffer.)]
    (while (not (= @i 0))
      (let [^int quotient (quot @i (int 10))
            ^int remainder (rem @i (int 10))]
        (.insert result (int 0) remainder)
        (reset! i quotient)))
    (.toString result)))

(defn -main ^{:t :void} [& _args]
  (format (int 2345))
  (println (format (int 2345))))
