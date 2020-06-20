(ns examples.a.demo01
  (:refer-clojure :exclude [format])
  (:require [kalai.common :refer :all]))

(defn format ^String [^Integer num]
      (let [^Integer i (atom num)
            ^StringBuffer result (atom (new-strbuf))]
           (while (not (= @i 0))
                  (let [^Integer quotient (quot @i 10)
                        ^Integer remainder (rem @i 10)]
                       (prepend-strbuf @result remainder)
                       (reset! i quotient)))
           (return (tostring-strbuf @result))))
