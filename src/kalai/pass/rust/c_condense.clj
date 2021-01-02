(ns kalai.pass.rust.c-condense
  (:require [meander.strategy.epsilon :as s]
            [meander.epsilon :as m]))

(def rewrite
  (s/bottom-up
    (s/rewrite
      ;;;; Raise unnecessarily nested blocks
      ;; {{body}} => {body}
      (r/block (r/block & ?more))
      (r/block & ?more)

      ;;;; Remove unnecessary operator grouping
      ;; TODO: is this ok for ALL operators??? seems ok for math, are there other operators?
      ;; (+ (+ x1 x2) y) => (+ x1 x2 y)
      (r/operator ?op (r/operator ?op . !xs ...) ?y)
      (r/operator ?op . !xs ... ?y)

      ;; (+ x (+ y1 y2)) => (+ x y1 y2)
      (r/operator ?op ?x (r/operator ?op . !ys ...))
      (r/operator ?op ?x . !ys ...)

      ;; (+ (+ x1 x2) (+ y1 y2)) => (+ x1 x2 y1 y2)
      (r/operator ?op (r/operator ?op . !xs ..?n) (r/operator ?op . !ys ..?m))
      (r/operator ?op . !xs ..?n !ys ..?m)

      ?else ?else)))
