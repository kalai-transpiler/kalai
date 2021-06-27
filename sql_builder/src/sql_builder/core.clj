(ns sql-builder.core
  (:refer-clojure :exclude [format])
  (:require [clojure.string :as str]))

(defn cast-to-str ^{:t :string} [^{:t :any} x]
  ^{:cast :string} x)

(defn select-str ^{:t :string} [^{:t {:mvector [:any]}} select]
  (str/join ", " (map cast-to-str select)))

(defn from-str ^{:t :string} [^{:t {:mvector [:any]}} from]
  (str/join ", " (map cast-to-str  from)))

(defn join-str ^{:t :string} [^{:t {:mvector [:any]}} join]
  (str/join ", " (map cast-to-str join)))

(defn where-str ^{:t :string} [^{:t :any} join]
  (if (vector? join)
    (let [;;^{:t :any} op (first join)
          ;;^{:t :any} more (rest join)
          ^{:t {:mvector [:any]}} jj ^{:cast :vector} join]
      (str "("
           (str/join (str " op ")
                     (map where-str jj))
           ")"))
    ^{:cast :string} join))

(defn group-by-str ^{:t :string} [^{:t {:mvector [:any]}} join]
  (str/join ", " (map cast-to-str join)))

(defn having-str ^{:t :string} [^{:t :any} having]
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
    ;; TODO: need to handle nil semantic or have a default value
    ;; for this example to work
    (str (if select
           (str "SELECT " (select-str select))
           "")
         (if from
           (str " FROM " (from-str from))
           "")
         (if join
           (str " JOIN " (join-str join))
           "")
         (if where-clause
           (str " WHERE " (where-str where-clause))
           "")
         (if group-by
           (str " GROUP BY " (group-by-str group-by))
           "")
         (if having
           (str " HAVING " (having-str having))
           ""))))
