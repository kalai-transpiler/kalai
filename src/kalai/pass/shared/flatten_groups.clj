(ns kalai.pass.shared.flatten-groups
  (:require [meander.strategy.epsilon :as s]
            [meander.epsilon :as m]))

(def rewrite
  (s/bottom-up
    (s/rewrite
      ((m/or (group . !stuff ...) !stuff) ...)
      (!stuff ...)

      ?else ?else)))
