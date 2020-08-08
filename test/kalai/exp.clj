(ns kalai.exp
  (:require [clojure.test :refer [deftest testing]]
            [kalai.placation :refer [is=]]))

(deftest t
  nil? nil

  (is= #"in 14" "in 1400 and 92")

  (is= "in 1400 and 92"
       "in 14OO and 92")

  (is= #{1 2} (conj #{} 1 3))

  (is= {:one 1 :many {:two 2}}
             (assoc {} :one 2 :many {:three 3})))
