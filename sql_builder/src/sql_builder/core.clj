(ns sql-builder.core
  (:refer-clojure :exclude [format])
  (:require [clojure.string :as str]))

(defn cast-to-str ^{:t :string} [^{:t :any} x]
  (if (vector? x)
    (let [^{:t {:mvector [:any]}} v ^{:cast :mvector} x
          ^{:t :any} v-first (nth v (int 0))
          ^{:t :string} table-name ^{:cast :string} v-first
          ^{:t :any} v-second (nth v (int 1))
          ^{:t :string} table-alias ^{:cast :string} v-second]
      (str table-name " AS " table-alias))
    ^{:cast :string} x))

(defn select-str ^{:t :string} [^{:t {:mvector [:any]}} select]
  (str/join ", " (map cast-to-str (seq select))))

(defn from-str ^{:t :string} [^{:t {:mvector [:any]}} from]
  (str/join ", " (map cast-to-str (seq from))))

(defn join-str ^{:t :string} [^{:t {:mvector [:any]}} join]
  (str/join ", " (map cast-to-str (seq join))))

;; TODO: honeySQL supports variadic clauses which are assumed to be `and`
(defn where-str ^{:t :string} [^{:t :any} clause]
  (if (vector? clause)
    (let [^{:t {:mvector [:any]}} v ^{:cast :mvector} clause
          ^{:t :any} v-first (first (seq v))
          ^{:t :string} op ^{:cast :string} v-first]
      (str "("
           (str/join (str " " op " ")
                     (map where-str (next (seq v))))
           ")"))
    ^{:cast :string} clause))

(defn group-by-str ^{:t :string} [^{:t {:mvector [:any]}} join]
  (str/join ", " (map cast-to-str (seq join))))

(defn having-str ^{:t :string} [^{:t :any} having]
  (where-str having))

(defn format
  "Converts query as data into an SQL string"
  ^{:t :string}
  [^{:t {:mmap [:string :any]}} query-map]
  (let [^{:t :any} select (get query-map :select nil)
        ^{:t :any} from (get query-map :from nil)
        ^{:t :any} join (get query-map :join nil)
        ^{:t :any} where-clause (get query-map :where nil)
        ^{:t :any} group-by (get query-map :group-by nil)
        ^{:t :any} having (get query-map :having nil)]
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
