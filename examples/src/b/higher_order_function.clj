(ns b.higher-order-function)

(defn -main ^{:t :void} [& _args]
  (let [x ^{:t {:mvector [:long]}} [1 2 3 4 5]]
    (println "HELLO***" (first (map (fn [^{:t :long} y] (+ y 1))
                                    x))))
  (let [y ^{:t {:mvector [:long]}} [1 2 3 4 5]
        ^long z (reduce (fn [a b] (+ a b)) y)
        ^String z2 (reduce (fn [a b] (str a b)) "" y)]
    (println "z =" z)
    (println "z2 =" z2)))
