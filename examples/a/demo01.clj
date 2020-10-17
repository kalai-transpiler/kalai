(ns a.demo01
  (:refer-clojure :exclude [format]))

(defn format ^String [^Integer num]
      (let [i (atom num)
            ^StringBuffer result (StringBuffer.)]
           (while (not (= @i 0))
                  (let [^int quotient (quot @i 10)
                        ^int remainder (rem @i 10)]
                       (.insert (int 0) result remainder)
                       (reset! i quotient)))
           (.toString result)))
