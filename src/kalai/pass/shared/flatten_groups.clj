(ns kalai.pass.shared.flatten-groups
  (:require [meander.strategy.epsilon :as s]
            [meander.epsilon :as m]))

(def rewrite
  (s/bottom-up
    (s/rewrite
      (m/and
        ((m/or (group . !stuff ...) !stuff) ...)
        (m/app meta ?meta))
      (m/app with-meta (!stuff ...) ?meta)

      ?else ?else)))
