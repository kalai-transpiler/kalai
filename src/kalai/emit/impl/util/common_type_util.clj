(ns kalai.emit.impl.util.common-type-util
  (:require [kalai.common :refer :all]
            [kalai.emit.interface :as iface :refer :all]
            [clojure.string :as string]
            [clojure.tools.analyzer.jvm :as az]))

(defn val-with-nesting?
  [v]
  (and (seqable? v)
       (not (string? v))))

(defn is-const-vector-nested?
  [ast-opts]
  (let [ast (:ast ast-opts)
        expr-form (:form ast)]
    (assert (seqable? expr-form))
    (boolean
     (some val-with-nesting? expr-form))))

(defn is-const-map-nested?
  [ast-opts]
  (let [ast (:ast ast-opts)
        expr-form (:form ast)]
    (assert (associative? expr-form))
    (boolean
     (or (some val-with-nesting? (vals expr-form))
         (some val-with-nesting? (keys expr-form))))))
