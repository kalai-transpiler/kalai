(ns kalai.types
  (:require [clojure.set :as set]
            [meander.epsilon :as m]
            [meander.strategy.epsilon :as s])
  (:import (clojure.lang IMeta)))

(def TYPE-MISSING-STR "TYPE_MISSING")
(def BAD-TYPE_CAST-STR "BAD_TYPE_CAST")

;; Primitive types in Kalai's supported types
(def primitive-types
  #{:int
    :long
    :float
    :double
    :bool
    :string
    :char ;; this is a bad idea for user facing strings, use ICU instead
    :byte})

;; Collection types in Kalai's supported types
(def generic-types
  #{:map
    :mmap
    :set
    :mset
    :vector
    :mvector
    :function})

;; All of Kalai's supported types
(def types
  (set/union primitive-types generic-types))

;; Conversion map between Java types (represented as Java classes and the
;; `TYPE` field of Java "boxed" classes for primitives) to Kalai types. Useful
;; for mapping/resolving possible type hints in Kalai source to a standard
;; Kalai type representation.
(def java-types
  {Integer      :int
   Integer/TYPE :int
   Long         :long
   Long/TYPE    :long
   Float        :float
   Float/TYPE   :float
   Double       :double
   Double/TYPE  :double
   Boolean      :bool
   Boolean/TYPE :bool
   String       :string
   Byte         :byte
   Byte/TYPE    :byte
   ;; TODO: Might not want any... Not all languages have any?
   Object       :any})

;; In Clojure symbols for primitives do not resolve to a type.
;; In the type hint ^long, long is a type, not the function (long).
;; We must provide a mapping for symbols representing primitive type hints.
(def primitive-symbol-types
  '{int     :int
    long    :long
    float   :float
    double  :double
    boolean :bool
    byte    :byte
    void    :void})

(def validate-kalai-type
  (s/rewrite
    (m/and
      {(m/pred generic-types ?t) [(m/pred validate-kalai-type !ts) ...]}
      ?generic-type)
    ?generic-type

    (m/pred primitive-types ?primitive-type)
    ?primitive-type

    :void
    :void

    ?else ~(throw (ex-info (str "Invalid Kalai type: " ?else ", T: " (type ?else)) {}))))

(defn get-kalai-type-from-java-type [tag]
  (or (get java-types tag)
      (throw (ex-info (str "Kalai does not recognize Java type hint " tag " of type " (type tag)) {:tag tag}))))

(defn get-kalai-type [metadata]
  (let [{:keys [t tag]} metadata]
    (or
      (and t (validate-kalai-type t))
      (and tag (get-kalai-type-from-java-type tag)))))

(defn source-location [^IMeta x]
  (let [{:keys [source line column]} (meta x)]
    (str source ":" line ":" column)))

(defn has-kalai-type [^IMeta x]
  (or (:t x)
      (throw (ex-info (str "Missed type annotation at " (source-location x))
                      {:meta (meta x)}))))

;; TODO: can we delete this now?
;; TODO: simplify this
(defn get-type [expr]
  (if (instance? IMeta expr)
    (let [{:keys [t]} (meta expr)]
      (or t
          (when (and (seq? expr) (seq expr))
            (case (first expr)
              ;; TODO: this suggests we need some type inference
              (j/new) (second expr)
              (j/block j/invoke do if) (get-type (last expr))
              (do
                (println "WARNING: missing type for" (pr-str expr))
                "MISSING_TYPE")))
          (when (not (symbol? expr))
            (type expr))
          (do (println "WARNING: missing type for" (pr-str expr))
              "MISSING_TYPE")))
    (get-kalai-type-from-java-type (type expr))))

(def lang-type-mappings
  {:kalai.emit.langs/rust {java.lang.StringBuffer '{:mvector [:char]}}})
