(ns kalai.pass.shared.raise-stuff
  (:require [meander.strategy.epsilon :as s]
            [meander.epsilon :as m]))

(def rewrite2
  (s/until
    =
    (s/rewrite
      (m/$ ?context (group . !x ... ?last))
      (j/block
        . !x ...
        ~(?context ?last))

      ?else
      ?else)))

(def statement
  (s/rewrite
    (j/block . !statements ...)
    (j/block . (m/app statement !statements) ...)

    (j/while ?condition . !statements ...)
    (m/app rewrite2
           (j/while ?condition .
                    (m/app statement !statements) ...))

    (j/foreach ?t ?sym ?xs ?body)
    (m/app rewrite2
           (j/foreach ?t ?sym ?xs
                      (m/app statement ?body)))

    ;; TODO: special case???
    (j/if ?condition ?then)
    (m/app rewrite2
           (j/if ?condition (m/app statement ?then)))

    (if ?condition ?then ?else)
    (m/app rewrite2
           (if ?condition (m/app statement ?then) (m/app statement ?else)))

    ?else
    (m/app rewrite2 ?else)))

(def maybe-function
  (s/rewrite
    (j/function ?name ?return-type ?docstring ?params
                (j/block . !statements ...))
    (j/function ?name ?return-type ?docstring ?params
                (j/block . (m/app statement !statements) ...))
    ?else ?else))

(def rewrite
  (s/rewrite
    (j/class ?name (j/block . !function ...))
    (j/class ?name (j/block . (m/app maybe-function !function) ...))

    ?else ~(throw (ex-info "fail" {:else ?else}))))


