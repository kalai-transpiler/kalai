(ns kalai.util
  (:require [meander.epsilon :as m]
            [meander.syntax.epsilon :as syntax]
            [meander.match.syntax.epsilon :as match]
            [puget.printer :as puget]))

(def c (atom 0))
(defn gensym2 [s]
  (symbol (str s (swap! c inc))))

(defn tmp [type]
  (with-meta (gensym2 "tmp") {:t type}))

(defn get-type [expr]
  (let [{:keys [t tag local]} (meta expr)]
    (or t
        tag
        (some-> local (get-type))
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
            "MISSING_TYPE"))))

(defn tmp-for [expr]
  (tmp (get-type expr)))

(defn spy
  ([x] (spy nil x))
  ([label x]
   (when label
     (println label))
   (doto x puget/cprint)))

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

(m/defsyntax var [v]
  (case (::syntax/phase &env)
    :meander/match
    `(m/app meta {:var ~v})
    &form))

(defn void? [expr]
  (#{:void 'void "void"} (get-type expr)))

(defn set-meta
  "If v is truthy, sets k to v in meta of x"
  [x k v]
  (if v
    (with-meta x (assoc (meta x) k v))
    x))
