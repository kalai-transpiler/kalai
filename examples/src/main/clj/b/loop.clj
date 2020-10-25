(ns b.loop)

(defn add ^{:t :int} [^{:t :int} a ^{:t :int} b]
  (dotimes [i 10]
    (println i))
  (doseq [^int ii ^:mut ^{:t {:vector [:int]}} [1 2 3]]
    (println ii))
  (let [x (atom (int 0))]
    (while (< @x 10)
      (println (inc @x)))
    (if true
      ;; TODO: this is cheating, we should know the type of x
      @^{:t :int}x
      (* 2 (+ 3 4) 5 6 7))))
