(ns kalai.pass.kalai.e-data-literals
  (:require [meander.strategy.epsilon :as s]
            [meander.epsilon :as m]))

(def rewrite
  (s/bottom-up
    (s/rewrite
      [!x ...]
      (persistent-vector . !x ...)

      (m/and {} (m/seqable [!k !v] ...))
      (persistent-map . !k !v ...)

      (m/and #{} (m/seqable !k ...))
      (persistent-set . !k ...)

      ?else ?else)))
