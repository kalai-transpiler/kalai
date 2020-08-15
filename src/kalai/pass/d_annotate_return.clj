(ns kalai.pass.d-annotate-return
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

(def maybe-function
  ;; TODO: this isn't quite right, functions only live in namespaces
  (s/rewrite
    (function ?name ?return-type ?docstring ?params . !statements ... ?return)
    (function ?name ?return-type ?docstring ?params . !statements ... (m/app return ?return))

    ?else ?else))

(def rewrite
  (s/rewrite
    (namespace ?name . (m/app maybe-function !f) ...)
    (namespace ?name . (m/app maybe-function !f) ...)))
