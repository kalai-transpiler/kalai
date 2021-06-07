(ns kalai.pass.rust.util
  (:require [kalai.types :as types]))

(defn preserve-type
  "Preserves the type information on the replacement expr"
  [expr replacement-expr]
  (with-meta
    replacement-expr
    (or (meta expr)
        (when-let [t (get types/java-types (type expr))]
          {:t t}))))

(defn clone
  "Preserves the type information while wrapping a value in a clone method"
  [expr]
  (preserve-type expr (list 'r/method 'clone expr)))


(defn literal? [x]
  (or (number? x)
      (string? x)
      (keyword? x)))

(defn wrap-value-enum [t x]
  (let [wrap-owned-expression (if (literal? x)
                                x
                                (clone x))]
    (if (= t :any)
      (list 'r/value wrap-owned-expression)
      wrap-owned-expression)))