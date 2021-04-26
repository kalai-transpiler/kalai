(ns sql-builder.core
  (:refer-clojure :exclude [format]))

(defn format
  ^{:t {:mvector [:string]}}
  [^{:t {:mmap [:string :string]}} query-map
   ^{:t {:mmap [:string :string]}} options]
  ^{:t {:mvector [:string]}} ["a" "b" "3"])

(defn format-no-opts
  ^{:t {:mvector [:string]}}
  [^{:t {:mmap [:string :string]}} query-map]
  (format query-map ^{:t {:mmap [:string :string]}} {}))
