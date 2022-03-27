(ns c.persistent)

(defn test-map ^Long []
  (let [^{:t {:mmap [:string :long]}} a {:x 11
                                         :y 13}
        ^{:t {:mmap [:any :any]}} b {:x 11
                                     :y 13}
        ^{:t {:map [:string :long]}} c {:x 11
                                        :y 13}
        ^{:t {:map [:any :any]}} d {:x 11
                                    :y 13}
        ^{:t :any} e ^{:t :any} {:x 11
                                 :y 13}]

    (println (str "key :y in mutable map a returns " (get a :y)))
    (let [^{:t :any} any-y :y
          ^{:t :long} get-b-any-y ^{:cast :long :t :any} (get b any-y)]
      (println (str "key :y in mutable map b returns " get-b-any-y)))
    (println (str "key :y in persistent map c returns " (get c :y)))
    (let [^{:t :any} any-y :y
          ^{:t :long} get-d-any-y ^{:cast :long :t :any} (get d any-y)]
      (println (str "key :y in persistent map d returns " get-d-any-y)))
    (let [^{:t :any} any-y :y
          ^{:t {:map [:any :any]}} e-map ^{:cast {:map [:any :any]}} e
          ^{:t :long} get-e-any-y ^{:cast :long :t :any} (get e-map any-y)]
      (println (str "key :y in persistent map e returns " get-e-any-y)))

    3))

(defn test-vector ^Long []
  (let [^{:t {:mvector [:long]}} a [11 13]
        ^{:t {:mvector [:any]}} b [11 13]
        ^{:t {:vector [:long]}} c [11 13]
        ^{:t {:vector [:any]}} d [11 13]]

    (println (str "size of mutable vector a returns " (count a)))
    (println (str "size of mutable vector b returns " (count b)))
    (println (str "size of persistent vector c returns " (count c)))
    (println (str "size of persistent vector d returns " (count d)))

    5))

(defn test-set ^Long []
  (let [^{:t {:mset [:long]}} a #{11 13 15}
        ^{:t {:mset [:any]}} b #{11 13 15}
        ^{:t {:set [:long]}} c #{11 13 15}
        ^{:t {:set [:any]}} d #{11 13 15}]

    (println (str "size of mutable set a returns " (count a)))
    (println (str "size of mutable set b returns " (count b)))
    (println (str "size of persistent set c returns " (count c)))
    (println (str "size of persistent set d returns " (count d)))

    7))

(defn -main ^{:t :void} [& _args]
  (println (test-map))
  (println (test-vector))
  (println (test-set)))
