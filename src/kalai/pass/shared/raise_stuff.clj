(ns kalai.pass.shared.raise-stuff
  (:require [meander.strategy.epsilon :as s]
            [meander.epsilon :as m]
            [kalai.util :as u]))

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
;; The catch when moving the contents of the group s-expression
;; to the higher/containing/parent scope is
;; that we need to know at which height to stop raising the group s-expression.

;; Considering an s-expression statement,
;; we recur in to find sub-statements,
;; upon reaching a statement with no sub-statements (a leaf statement),
;; we raise all contained groups to an enclosing group,
;; the enclosing group is then ready to be raised when the same operation
;; is applied to it's parent statement as the recursion unwinds and continues.

;; For the purposes of this description,
;; the term statement excludes group expressions which contain statements.
;; A group of statements will not be matched as a statement,
;; and so it's children will not be recurred upon.

;; statements recur (can have child statements).
;; raise-or-splice does not recur, but it does work bottom up,
;; so modification to the inner forms affect matching of outer forms.

;; Group raising bubbles up to the parent statement until we are left with
;; a single enclosing group per parent statement.
;; This group is now ready to be stripped away by flatten-groups.
;; The temporary variables are at the same level as the statement that uses them.


(def raise-or-splice
  (s/rewrite
    ;; if there is an enclosing group already established, just splice
    (group . (m/or (group . !x ...)
                   !x)
           ...)
    (group . !x ...)

    ;; Preserve short circuit evaluation.
    ;; temp variables must not escape if, so prevent that.
    ;; Branches must be in blocks as they can contain groups!
    ;; The blocks preserve the indivisibility of the contained statements.
    ;; In contrast, groups allow the statements to be divided, for example:
    ;; the temp variable expression vs the initialization statements prior to it.
    (j/if ?condition ?then)
    (j/if ?condition
      (j/block ?then))

    (j/if ?condition ?then ?else)
    (j/if ?condition
      (j/block ?then)
      (j/block ?else))

    ;; establish an enclosing group,
    ;; and raise temporary variable initialization
    (m/and ((m/or (group . !tmp-init ... !tmp-variable)
                  !tmp-variable)
            ...)
           ?expr)
    (group . !tmp-init ... (m/app u/preserve-type ?expr (!tmp-variable ...)))

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
    (j/function ?name ?params
                (j/block . !statements ...))
    (j/function ?name ?params
                (j/block . (m/app statement !statements) ...))

    ?else
    (m/app raise-sub-groups ?else)))

(def rewrite
  (s/rewrite
    ;; only support top level functions because we can't guarantee that
    ;; target languages support top level statements if data literals
    ;; occur in top level defs
    (j/class ?name (j/block . !function ...))
    (j/class ?name (j/block . (m/app maybe-function !function) ...))

    ?else
    ;; TODO: uncomment exception, make copy of this for each target language until
    ;; we figure out a better solution
    ;;~(throw (ex-info "Raise namespace" {:else ?else}))
    ?else
    ))


