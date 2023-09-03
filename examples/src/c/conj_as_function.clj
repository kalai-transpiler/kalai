(ns c.conj-as-function
  (:require [clojure.test :refer :all]))

(defn conj-map-map ^Long []
  (let [a ^{:t :any} {:a 1}
        b ^{:t :any} {:b 2}
        ^{:t :any} c (conj a b)]
    ;; if we implement a count helper we can solve this
    ;;(count c)
    3))

(defn conj-map-vec ^Long []
  (let [a ^{:t :any} {:a 1}
        b ^{:t :any} [:b 2]
        ^{:t :any} c (conj a b)]
    ;; if we implement a count helper we can solve this
    ;;(count c)
    11))

(defn conj-set ^Long []
  (let [a ^{:t :any} #{:a :b}
        ^{:t :any} new-value :c
        ^{:t :any} c (conj a new-value)]
    ;; if we implement a count helper we can solve this
    ;;(count c)
    5))

(defn conj-vector ^Long []
  (let [a ^{:t :any} [:a :b]
        ^{:t :any} new-value :c
        ^{:t :any} c (conj a new-value)]
    ;; if we implement a count helper we can solve this
    ;;(count c)
    7))


;; Demonstrates that conj cannot be used with specific types,
;; but can be used with `{:map [:any :any]}`
(defn type-conversions ^Long []
  (let [a ^{:t {:map [:any :any]}} {:a 1}
        b ^{:t {:map [:any :any]}} {:b 1}
        ;sb ^{:t {:map [:string :long]}} {:b 1}
        ]
    ;; a can be cast to any because we have a conversion defined
    ;; between any and map<any,any>
    ;;^{:cast :any} a

    ;; sb cannot be cast to any because we do not have a specific conversion for
    ;; any to map<string,int> (Maybe we could have generated it)
    ;;^{:cast :any} sb

    (conj ^{:cast :any} a ^{:cast :any} b)
    4
    ))

(defn -main ^{:t :void} [& _args]
  (println (conj-map-map))
  (println (conj-map-vec))
  (println (conj-set))
  (println (conj-vector))
  (println (type-conversions)))
