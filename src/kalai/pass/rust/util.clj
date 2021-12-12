(ns kalai.pass.rust.util
  (:require [kalai.types :as types]
            [kalai.util :as u]
            [clojure.string :as str]))

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

;;
;; symbol -> string fns (refactored from e-string)
;;

(defn identifier
  "For Rust, do a lowercase snake-case, unless it is already uppercased
  (ex: struct or type name), in which case, just return as-is."
  [s]
  (let [s-str (str s)
        id-first-char (first s-str)]
    (if (Character/isUpperCase ^char id-first-char)
      s-str
      (let [snake-case (u/->snake_case s-str)]
        (if (= \_ (first s-str))
          (str \_ snake-case)
          snake-case)))))

(defn fully-qualified-function-identifier-str [function-name]
  (if (string? function-name)
    function-name
    (let [varmeta (some-> function-name meta :var meta)]
      (if (and (str/includes? (str function-name) "/") varmeta)
        ;; For now, we interpret the "/" to indicate that the function being transpiled is
        ;; either from Kalai or the user, and therefore, it has a namespace. We need to
        ;; handle the Rust snake-casing segment-by-segment when applying `identifier`.
        (let [clojure-ns-by-dot (str/split (str (:ns varmeta)) #"\.")
              rustified-ns-by-dot (map identifier clojure-ns-by-dot)
              rustified-ns (str/join "." rustified-ns-by-dot)]
          (str "crate::"
               (str/replace rustified-ns "." "::") ;; we use varmeta because we want the full ns, not an alias
               "::" (identifier (:name varmeta))))
        (str (identifier function-name))))))
