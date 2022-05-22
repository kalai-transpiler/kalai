(ns b.higher-order-function)

(defn -main ^{:t :void} [& _args]
  (let [x ^{:t {:mvector [:long]}} [1 2 3 4 5]]
    (println "HELLO***" (first (map (fn [^{:t :long} y] (+ y 1))
                                    x)))))
