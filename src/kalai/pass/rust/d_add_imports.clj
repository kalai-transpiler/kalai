(ns kalai.pass.rust.d-add-imports
  (:require [meander.strategy.epsilon :as s]))

(def m
  '{r/hash-map PersistentMap
    r/assoc PersistentMap
    PersistentMap PersistentMap})

(defn imports-for [c]
  (set (map m (distinct (flatten c)))))

(def rewrite
  #_(s/rewrite
    (m/and
      ?c
      (r/class
        . x! ...))
    ;;->
    (r/class
      ~(imports-for ?c)
      . x! ...)))
