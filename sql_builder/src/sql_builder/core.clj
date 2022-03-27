(ns sql-builder.core
  (:refer-clojure :exclude [format])
  (:require [clojure.string :as str]))

(defn cast-to-str ^{:t :string} [^{:t :any} x]
  (if (vector? x)
    (let [^{:t {:mvector [:any]}} v ^{:cast {:mvector [:any]}} x
          ^{:t :any} v-first (nth v (int 0))
          ^{:t :string} table-name ^{:cast :string} v-first
          ^{:t :any} v-second (nth v (int 1))
          ^{:t :string} table-alias ^{:cast :string} v-second]
      (str table-name " AS " table-alias))
    (if (string? x)
      (str ^{:cast :string} x)
      (if (instance? Integer x)
        (str ^{:cast :int} x)
        (if (instance? Long x)
          (str ^{:cast :long} x)
          "")))))

(defn select-str ^{:t :string} [^{:t {:mvector [:any]}} select]
  (str/join ", " (map cast-to-str (seq select))))

(defn from-str ^{:t :string} [^{:t {:mvector [:any]}} from]
  (str/join ", " (map cast-to-str (seq from))))

(defn join-str ^{:t :string} [^{:t {:mvector [:any]}} join]
  (str/join ", " (map cast-to-str (seq join))))

;; TODO: honeySQL supports variadic clauses which are assumed to be `and`
(defn where-str ^{:t :string} [^{:t :any} clause]
  (if (vector? clause)
    (let [^{:t {:mvector [:any]}} v ^{:cast {:mvector [:any]}} clause
          ^{:t :any} v-first (first (seq v))
          ^{:t :string} op ^{:cast :string} v-first]
      (str "("
           (str/join (str " " op " ")
                     (map where-str (next (seq v))))
           ")"))
    (cast-to-str clause)))

(defn group-by-str ^{:t :string} [^{:t {:mvector [:any]}} join]
  (str/join ", " (map cast-to-str (seq join))))

(defn having-str ^{:t :string} [^{:t :any} having]
  (where-str having))

(defn row-str ^{:t :string} [^{:t :any} row]
  (let [^{:t {:mvector [:any]}} mrow ^{:cast {:mvector [:any]}} row]
    (str "(" (str/join ", " (map cast-to-str (seq mrow))) ")")))

(defn format
  "Converts query as data into an SQL string"
  ^{:t :string}
  [^{:t {:mmap [:string :any]}} query-map]
  (let [^{:t :any} select (get query-map :select nil)
        ^{:t :any} from (get query-map :from nil)
        ^{:t :any} join (get query-map :join nil)
        ^{:t :any} where-clause (get query-map :where nil)
        ^{:t :any} group-by (get query-map :group-by nil)
        ^{:t :any} having (get query-map :having nil)
        ^{:t :any} insert-into (get query-map :insert-into nil)
        ^{:t :any} columns (get query-map :columns nil)
        ^{:t :any} values (get query-map :values nil)]
    ;; TODO: need to handle nil semantic or have a default value
    ;; for this example to work
    (str (if (nil? insert-into)
           ""
           (str "INSERT INTO " (from-str ^{:cast {:mvector [:any]}} insert-into) "(" (select-str ^{:cast {:mvector [:any]}} columns) ")\n"
                "VALUES\n"
                (str/join ",\n" (let [^{:t {:mvector [:any]}} v2 ^{:cast {:mvector [:any]}} values]
                                  (map row-str (seq v2))))))

         (if (nil? select)
           ""
           (str "SELECT " (select-str ^{:cast {:mvector [:any]}} select)))
         (if (nil? from)
           ""
           (str " FROM " (from-str ^{:cast {:mvector [:any]}} from)))
         (if (nil? join)
           ""
           (str " JOIN " (join-str ^{:cast {:mvector [:any]}} join)))
         (if (nil? where-clause)
           ""
           (str " WHERE " (where-str where-clause)))
         (if (nil? group-by)
           ""
           (str " GROUP BY " (group-by-str ^{:cast {:mvector [:any]}} group-by)))
         (if (nil? having)
           ""
           (str " HAVING " (having-str having))))))
