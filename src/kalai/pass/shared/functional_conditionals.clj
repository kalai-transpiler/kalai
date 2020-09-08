(ns kalai.pass.shared.functional-conditionals
  (:require [meander.strategy.epsilon :as s]
            [meander.epsilon :as m]))

;; start at the top and find the first `if`,
;; create a block:
;;   declare a variable,
;;   if, assignment,
;;   make use of the result in the final expr
;; recursive,
;; applies to statements and return (return is a statement)
;; 4 options: (if c (do)), (if c (do) else), (if c then (do)), (if c (do) (do))
(def unwrap-result
  (s/rewrite
    (m/let [?tmp (gensym "tmp")]
      (m/$ ?context (if ?c (do . !x ... ?last))))
    (group
      (init ?tmp)
      (if ?c
        (do . !x ... (m/app unwrap-result (assign ?tmp ?last))))
      ~(?context ?tmp))

    (m/let [?tmp (gensym "tmp")]
      (m/$ ?context (if ?c (do . !x ... ?last) ?else)))
    (group
      (init ?tmp)
      (if ?c
        (do . !x ... (m/app unwrap-result (assign ?tmp ?last)))
        ?else)
      ~(?context ?tmp))

    (m/let [?tmp (gensym "tmp")]
      (m/$ ?context (if ?c ?then (do . !x ... ?last))))
    (group
      (init ?tmp)
      (if ?c
        ?then
        (do . !x ... (m/app unwrap-result (assign ?tmp ?last))))
      ~(?context ?tmp))


    (m/let [?tmp (gensym "ktmp")]
      (m/$ ?context (if ?c (do . !x ... ?last1) (do . !y ... ?last2))))
    (group
      (init ?tmp)
      (if ?c
        (do . !x ... (m/app unwrap-result (assign ?tmp ?last1)))
        (do . !y ... (m/app unwrap-result (assign ?tmp ?last2))))
      ~(?context ?tmp))

    ?else
    ?else))

(comment
  (unwrap-result '(+ (if true (do 1 2)) 4))
  (unwrap-result '(+ (if true (do 1 (if false (do 2 3)))) 4))
  (unwrap-result '(+ (if true (do 1 (if false (do 2 3)))
                              (do 4 5)))))
