(ns kalai.pass.kalai.e-data-literals
  (:require [meander.strategy.epsilon :as s]
            [meander.epsilon :as m]))

(def rewrite
  ;; We use top down instead of bottom up here because top down does not
  ;; change the ordering of elements in the collection as they occur.
  ;; But with bottom up we see for example #{4 [5 6]} after the first step
  ;; would change into (persistent-set (persistent-vector 5 6) 4)
  ;; and therefore it introduces a reordering of corresponding elements.
  ;; Data literals will incur non-determinism with map/set ordering.
  (s/top-down
    (s/rewrite
      [!x ...]
      (persistent-vector . !x ...)

      (m/and {} (m/seqable [!k !v] ...))
      (persistent-map . !k !v ...)

      (m/and #{} (m/seqable !k ...))
      (persistent-set . !k ...)

      ?else ?else)))
