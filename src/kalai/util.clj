(ns kalai.util
  (:require [meander.epsilon :as m]
            [meander.syntax.epsilon :as syntax]
            [meander.match.syntax.epsilon :as match]))

(defn match-type? [t x]
  (or
    (some-> x
            meta
            (#(or (= t (:t %))
                  (= t (:tag %)))))
    (= t (type x))))

(m/defsyntax of-type [t x]
  (case (::syntax/phase &env)
    :meander/match
    `(match/pred #(match-type? ~t %) ~x)
    &form))

(defn get-type [expr]
  (let [{:keys [t tag]} (meta expr)]
    (or t
        tag
        ;; TODO: only for do and let, not function call
        (when (and (seq? expr) (seq expr))
          (get-type (last expr)))
        (type expr))))

(defn void? [expr]
  (#{:void 'void "void"} (get-type expr)))
