(ns kalai.pass.shared.raise-stuff
  (:require [meander.strategy.epsilon :as s]
            [meander.epsilon :as m]))

;; The reason this pipeline pass exists is because
;; flatten-groups only splices out group s-expressions.
;; However, there is a cardinality mismatch between
;; the number of forms that a group s-expression contains
;; (by construction more than 1).
;; This causes a problem in situations like init which expects
;; a single expression in a particular position,
;; not multiple statements.
;; Those multiple statements need to be moved to precede the init statement.
;; We use a temp variable where the group used to be.
;; The catch when moving the contents of the group s-expresssion
;; to the higher/containing/parent scope is
;; that we need to know when to stop the recursion.

(def raise-or-splice
  (s/rewrite
    ;; if there is an enclosing group already established, just splice
    (group . !before ...
           (group . !tmp-init ... ?tmp-variable)
           . !after ...)
    (group . !before ...
           . !tmp-init ...
           ?tmp-variable
           . !after ...)

    ;; establish an enclosing group,
    ;; and raise temporary variable initialization
    (!before ... (group . !tmp-init ... ?tmp-variable) . !after ...)
    (group
      . !tmp-init ...
      (!before ... ?tmp-variable . !after ...))

    ?else
    ?else))

(def raise-sub-groups
  (s/bottom-up raise-or-splice))

(def statement
  (s/rewrite
    (j/block . !statements ...)
    (j/block . (m/app statement !statements) ...)

    (j/while ?condition . !statements ...)
    (m/app raise-sub-groups
           (j/while ?condition .
                    (m/app statement !statements) ...))

    (j/foreach ?t ?sym ?xs ?body)
    (m/app raise-sub-groups
           (j/foreach ?t ?sym ?xs
                      (m/app statement ?body)))

    ;; TODO: special case???
    (j/if ?condition ?then)
    (m/app raise-sub-groups
           (j/if ?condition (m/app statement ?then)))

    (if ?condition ?then ?else)
    (m/app raise-sub-groups
           (if ?condition (m/app statement ?then) (m/app statement ?else)))

    ?else
    (m/app raise-sub-groups ?else)))

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


