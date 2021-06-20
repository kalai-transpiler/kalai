(ns sql-builder.core
  (:refer-clojure :exclude [format])
  (:require [clojure.string :as str]))

(defn select-str [select]
  (str/join ", " select))

(defn from-str [from]
  (str/join ", " from))

(defn join-str [join]
  (str/join ", " join))

(defn where-str [join]
  (if (vector? join)
    (let [op (first join)
          more (rest join)]
      (str "("
           (str/join (interpose (str " " op " ")
                                (map where-str more)))
           ")"))
    join))

(defn group-by-str [join]
  (str/join ", " join))

(defn having-str [having]
  (where-str having))

(defn format
  "Converts query as data into an SQL string"
  ^{:t :string}
  [^{:t {:mmap [:string {:mvector [:any]}]}} query-map]
  (let [select (:select query-map)
        from (:from query-map)
        join (:join query-map)
        where-clause (:where query-map)
        group-by (:group-by query-map)
        having (:having query-map)]
    (str (when select (str "SELECT " (select-str select)))
         (when from (str " FROM " (from-str from)))
         (when join (str " JOIN " (join-str join)))
         (when where-clause (str " WHERE " (where-str where-clause)))
         (when group-by (str " GROUP BY " (group-by-str group-by)))
         (when having (str " HAVING " (having-str having))))))
