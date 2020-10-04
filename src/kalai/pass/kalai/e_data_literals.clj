(ns kalai.pass.kalai.e-data-literals
  (:require [meander.strategy.epsilon :as s]
            [meander.epsilon :as m]))

(defn top-down-to
  "Build a strategy which applies `s` to each subterm of `t` from
  top to bottom until `pred` is true for some subterm of `t`.
  That subterm will have the strategy applied,
  but subterms of the subterm will not."
  [pred s]
  (fn rec [t]
    (if (pred t)
      (s t)
      ((s/pipe s (s/all rec)) t))))

(def rewrite
  ;; We use top down instead of bottom up here because top down does not
  ;; change the ordering of elements in the collection as they occur.
  ;; But with bottom up we see for example #{4 [5 6]} after the first step
  ;; would change into (persistent-set (persistent-vector 5 6) 4)
  ;; and therefore it introduces a reordering of corresponding elements.
  ;; Data literals will incur non-determinism with map/set ordering.
  (top-down-to
    (s/rewrite (preserve . _ ...) true _ false)
    (s/rewrite
      ;; function params are vectors, we don't want to replace those
      (preserve ?x)
      ?x

      [!x ...]
      (persistent-vector . !x ...)

      (m/and {} (m/seqable [!k !v] ...))
      (persistent-map . !k !v ...)

      (m/and #{} (m/seqable !k ...))
      (persistent-set . !k ...)

      ?else ?else)))
