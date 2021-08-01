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

;; TODO: honeySQL supports variadic clauses which are assumed to be `and`
(defn where-str ^{:t :string} [^{:t :any} clause]
  (if (vector? clause)
    (let [^{:t {:mvector [:any]}} v ^{:cast :mvector} clause
          op (first v)
          more (next v)
          ;;[op & more] v
          ]
      (str "("
           (str/join (str " " op " ")
                     (map where-str more))
           ")"))
    ^{:cast :string} clause))

(defn group-by-str ^{:t :string} [^{:t {:mvector [:any]}} join]
  (str/join ", " (map cast-to-str join)))

(defn having-str ^{:t :string} [^{:t :any} having]
  (where-str having))

(defn format
  "Converts query as data into an SQL string"
  ^{:t :string}
  [^{:t {:mmap [:string :any]}} query-map]
  (let [^{:t :any} select (:select query-map)
        ^{:t :any} from (:from query-map)
        ^{:t :any} join (:join query-map)
        ^{:t :any} where-clause (:where query-map)
        ^{:t :any} group-by (:group-by query-map)
        ^{:t :any} having (:having query-map)]
    ;; TODO: need to handle nil semantic or have a default value
    ;; for this example to work
    (str (if (nil? select)
           ""
           (str "SELECT " (select-str ^{:cast :mvector} select)))
         (if (nil? from)
           ""
           (str " FROM " (from-str ^{:cast :mvector} from)))
         (if (nil? join)
           ""
           (str " JOIN " (join-str ^{:cast :mvector} join)))
         (if (nil? where-clause)
           ""
           (str " WHERE " (where-str where-clause)))
         (if (nil? group-by)
           ""
           (str " GROUP BY " (group-by-str ^{:cast :mvector} group-by)))
         (if (nil? having)
           ""
           (str " HAVING " (having-str having))))))
