(ns c.persistent)

(defn test-conj ^Long []
  (let [^{:t {:map [:string :long]}} y {:x 11
                                       :y 13}]
    3))

(defn -main ^{:t :void} [& _args]
  (println (test-conj)))
