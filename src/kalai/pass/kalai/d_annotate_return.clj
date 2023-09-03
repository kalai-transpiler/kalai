(ns kalai.pass.kalai.d-annotate-return
  (:require [kalai.util :as u]
            [meander.strategy.epsilon :as s]
            [meander.epsilon :as m]))

(def return
  (s/rewrite
    (do . !statements ... ?last)
    (do . !statements ... (m/app return ?last))

    (while ?condition . !statements ...)
    (group
      (while ?condition . !statements ...)
      (return nil))

    (foreach ?t ?sym ?xs ?body)
    (group
      (foreach ?t ?sym ?xs ?body)
      (return nil))

    (if ?condition ?then)
    (if ?condition (m/app return ?then) (m/app return nil))

    (if ?condition ?then ?else)
    (if ?condition (m/app return ?then) (m/app return ?else))

    ;; TODO: I think group is redundant with do
    (group . !statements ... ?last)
    ;; TODO: does annotating statements help?
    (group . !statements ... (m/app return ?last))

    (return ?expression)
    (return ?expression)

    ?else
    (return ?else)))

(def rewrite
  (s/bottom-up
    (s/rewrite
      (function ?name (m/pred #(not= :void (:t (meta %))) ?params) . !statements ... ?last)
      (function ?name ?params . !statements ... (m/app return ?last))

      (lambda (m/pred #(not= :void (:t (meta %))) ?params) . !statements ... ?last)
      (lambda ?params . !statements ... (m/app return ?last))

      ?else
      ?else)))
