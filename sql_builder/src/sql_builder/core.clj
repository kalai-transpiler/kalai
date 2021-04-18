(ns sql-builder.core
  (:refer-clojure :exclude [format]))

(defn format
  [query-map options]
  ["a" "b" "3"])

(defn format-no-opts
  [query-map]
  (format query-map {}))
