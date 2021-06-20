(ns sql-builder.core
  (:refer-clojure :exclude [format])
  (:require [clojure.string :as str]))

(defn select-str ^{:t :string} [^{:t {:mvector [:any]}} select]
  (str/join ", " select))

(defn from-str ^{:t :string} [^{:t {:mvector [:any]}} from]
  (str/join ", " from))

(defn join-str ^{:t :string} [^{:t {:mvector [:any]}} join]
  (str/join ", " join))

(defn where-str ^{:t :string} [^{:t :any} join]
  (if (vector? join)
    (let [^{:t :any} op (first join)
          ^{:t :any} more (rest join)]
      (str "("
           (str/join (str " " op " ")
                     (map where-str more))
           ")"))
    join))

(defn group-by-str ^{:t :string} [^{:t {:mvector [:any]}} join]
  (str/join ", " join))

(defn having-str ^{:t :string} [^{:t {:mvector [:any]}} having]
  (where-str having))

(defn format
  "Converts query as data into an SQL string"
  ^{:t :string}
  [^{:t {:mmap [:string {:mvector [:any]}]}} query-map]
  (let [^{:t {:mvector [:any]}} select (:select query-map)
        ^{:t {:mvector [:any]}} from (:from query-map)
        ^{:t {:mvector [:any]}} join (:join query-map)
        ^{:t {:mvector [:any]}} where-clause (:where query-map)
        ^{:t {:mvector [:any]}} group-by (:group-by query-map)
        ^{:t {:mvector [:any]}} having (:having query-map)]
    (str (when select (str "SELECT " (select-str select)))
         (when from (str " FROM " (from-str from)))
         (when join (str " JOIN " (join-str join)))
         (when where-clause (str " WHERE " (where-str where-clause)))
         (when group-by (str " GROUP BY " (group-by-str group-by)))
         (when having (str " HAVING " (having-str having))))))
