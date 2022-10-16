(ns c.conj-as-function
  (:require [clojure.test :refer :all]))

(defn test-map ^Long []
  (let [a ^{:t :any} {:a 1}
        b ^{:t :any} {:b 2}
        ^{:t :any} c (conj a b)]
    3))

(defn -main ^{:t :void} [& _args]
  (println (test-map)))