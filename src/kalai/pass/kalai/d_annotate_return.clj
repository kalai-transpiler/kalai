(ns kalai.pass.kalai.d-annotate-return
  (:require [meander.strategy.epsilon :as s]
            [meander.epsilon :as m]))

(def return
  (s/rewrite
    (do . !statements ... ?last)
    (do . !statements ... (m/app return ?last))

    (while ?condition . !statements ...)
    (group
      (while ?condition . !statements ...)
      (return nil))

    (foreach ?bindings . !statements ...)
    (group
      (foreach ?bindings . !statements ...)
      (return nil))

    (if ?condition ?then)
    (if ?condition (m/app return ?then))

    (if ?condition ?then ?else)
    (if ?condition (m/app return ?then) (m/app return ?else))

    (init ?name ?value)
    (init ?name ?value)

    (group . !expession ... ?last)
    (group . !expession ... (m/app return ?last))

    (return ?expression)
    (return ?expression)

    ?else (return ?else)))

(def maybe-function
  (s/rewrite
    (function ?name ?return-type ?docstring ?params . !statements ... ?last)
    (function ?name ?return-type ?docstring ?params . !statements ... (m/app return ?last))

    ?else ?else))

(def rewrite
  (s/rewrite
    (namespace ?name . !function ...)
    (namespace ?name . (m/app maybe-function !function) ...)))
