(ns examples.b.loop
  (:require [kalai.common :refer :all]
            [clojure.tools.analyzer.passes.jvm.emit-form :as e]
            [clojure.tools.analyzer.jvm :as az]))

(defn add ^Long [^Long a ^Long b]
  (let [x (atom 0)]
    (while (< @x 10)
      (println (inc @x))
      #_(var-set x (inc @x)))
    (if true @x (* 1 (+ 2 3) 4 5 6 7))))
