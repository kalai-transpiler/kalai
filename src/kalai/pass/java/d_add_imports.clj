(ns kalai.pass.java.d-add-imports
  (:require [meander.strategy.epsilon :as s]))

(def m
  '{j/hash-map PersistentMap
    j/assoc PersistentMap
    PersistentMap PersistentMap})

(defn imports-for [c]
  (set (map m (distinct (flatten c)))))

(def rewrite
  #_(s/rewrite
    (m/and
      ?c
      (j/class
        . x! ...))
    ;;->
    (j/class
      ~(imports-for ?c)
      . x! ...)))
