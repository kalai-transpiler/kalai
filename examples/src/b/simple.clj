(ns b.simple)

(defn add ^Long [^Long a ^Long b]
      (+ a b))

(defn -main ^{:t :void} [& _args]
      (println (add 1 2)))
