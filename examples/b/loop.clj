(ns b.loop)

(defn add ^Long [^Long a ^Long b]
  (doseq [i 10]
    (println i))
  (let [x (atom 0)]
    (while (< @x 10)
      (println (inc @x))
      #_(var-set x (inc @x)))
    (if true @x (* 1 (+ 2 3) 4 5 6 7))))
