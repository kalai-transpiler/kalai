(ns b.loops)

(defn -main ^{:t :void} [& _args]
  (dotimes [^{:t :int} i1 (int 10)]
    (println i1))
  (dotimes [^{:t :long} i2 10]
    (println i2))
  (doseq [^long ii ^:mut ^{:t {:mvector [:long]}} [1 2 3]]
    (println ii))
  (let [x (atom 0)]
    (while (< @x 10)
      (reset! x (inc @x))
      (println @x))))
