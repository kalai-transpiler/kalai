(ns b.variable)

(defn side-effect ^Long []
  (let [y (atom 2)]
    (reset! y 3)
    (swap! y + 4)
    @y))

(defn -main ^{:t :void} [& _args]
  (println (side-effect)))
