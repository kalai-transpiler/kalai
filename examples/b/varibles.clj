(ns examples.b.variables
      (:require [kalai.common :refer :all]))

(defn side-effect ^Long []
      (let [x 2]
           (return x))
      (let [y (atom 2)]
           (reset! y 3)
           (swap! y + 4)))
