(ns examples.b.loop
      (:require [kalai.common :refer :all]))

(defn add ^Long [^Long a ^Long b]
      (let [x (atom 0)]
        (while (< @x 10)
               (println)
               (return (inc @x))
               #_(var-set x (inc @x)))))
