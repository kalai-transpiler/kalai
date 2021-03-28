(ns kalai.pass.java.c-condense
  (:require [meander.strategy.epsilon :as s]
            [meander.epsilon :as m]))

(def rewrite
  (s/bottom-up
    (s/rewrite
      ;;;; Raise unnecessarily nested blocks
      ;; {{body}} => {body}
      (j/block (j/block & ?more))
      (j/block & ?more)

      ;;;; Remove unnecessary operator grouping
      ;; TODO: is this ok for ALL operators??? seems ok for math, are there other operators?
      ;; (+ (+ x1 x2) y) => (+ x1 x2 y)
      (j/operator ?op (j/operator ?op . !xs ...) ?y)
      (j/operator ?op . !xs ... ?y)

      ;; (+ x (+ y1 y2)) => (+ x y1 y2)
      (j/operator ?op ?x (j/operator ?op . !ys ...))
      (j/operator ?op ?x . !ys ...)

      ;; (+ (+ x1 x2) (+ y1 y2)) => (+ x1 x2 y1 y2)
      (j/operator ?op (j/operator ?op . !xs ..?n) (j/operator ?op . !ys ..?m))
      (j/operator ?op . !xs ..?n !ys ..?m)

      ?else ?else)))
