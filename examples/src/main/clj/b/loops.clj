(ns b.loops)

(defn -main ^{:t :void} [& _args]
  (dotimes [i 10]
    (println i))
  (doseq [^int ii ^:mut ^{:t {:mvector [:int]}} [1 2 3]]
    (println ii))
  (let [x (atom (int 0))]
    (while (< @x 10)
      (reset! x (inc @x))
      (println @x))))
