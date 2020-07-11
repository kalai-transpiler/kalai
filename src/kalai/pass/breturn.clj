(ns kalai.pass.breturn
  (:require [meander.strategy.epsilon :as s]
            [meander.epsilon :as m]))

(def return
  (s/rewrite
    (do . !statements ... ?return)
    (do . !statements ... (m/app return ?return))

    (if ?condition ?then)
    (if ?condition (m/app return ?then))

    (if ?condition ?then ?else)
    (if ?condition (m/app return ?then) (m/app return ?else))

    (return ?expression)
    (return ?expression)

    ?else (return ?else)))

(def annotate-returns
  (s/rewrite
    (function ?return-type ?name ?docstring ?params . !statements ... ?return)
    (function ?return-type ?name ?docstring ?params . !statements ... (m/app return ?return))

    ?else ?else))
