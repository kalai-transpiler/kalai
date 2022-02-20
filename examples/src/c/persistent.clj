(ns c.persistent)

(defn test-conj ^Long []
  (let [^{:t {:mmap [:string :long]}} a {:x 11
                                         :y 13}
        ^{:t {:mmap [:any :any]}} b {:x 11
                                     :y 13}
        ^{:t {:map [:string :long]}} c {:x 11
                                        :y 13}
        ^{:t {:map [:any :any]}} d {:x 11
                                    :y 13}]
    3))

(defn -main ^{:t :void} [& _args]
  (println (test-conj)))
