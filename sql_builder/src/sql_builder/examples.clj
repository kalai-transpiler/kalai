(ns sql-builder.examples
  (:require [sql-builder.core :as sql]))

(defn f1 ^{:t :string} []
  (let [^{:t {:mmap [:string :any]}} query-map {:select ^{:t {:mvector [:any]}} ["a" "b" "c"]
                                                :from   ^{:t {:mvector [:any]}} ["foo"]
                                                :where  ^{:t {:mvector [:any]}} ["=" "f.a" "'baz'"]}]
    (sql/format query-map)))

(defn f2 ^{:t :string} []
  (let [^{:t {:mmap [:string :any]}} query-map {:select ^{:t {:mvector [:any]}} ["*"]
                                                :from   ^{:t {:mvector [:any]}} ["foo"]
                                                :where  ^{:t {:mvector [:any]}} ["AND"
                                                                                 ^{:t {:mvector [:any]}} ["=" "a" 1]
                                                                                 ^{:t {:mvector [:any]}} ["<" "b" 100]]}]
    (sql/format query-map)))

(defn f3 ^{:t :string} []
  (let [^{:t {:mmap [:string :any]}} query-map {:select ^{:t {:mvector [:any]}} ["a"
                                                                                 ^{:t {:mvector [:any]}} ["b" "bar"]
                                                                                 "c"
                                                                                 ^{:t {:mvector [:any]}} ["d" "x"]]
                                                :from   ^{:t {:mvector [:any]}} [^{:t {:mvector [:any]}} ["foo" "quux"]]
                                                :where  ^{:t {:mvector [:any]}} ["AND"
                                                                                 ^{:t {:mvector [:any]}} ["=" "quux.a" 1]
                                                                                 ^{:t {:mvector [:any]}} ["<" "bar" 100]]}]
    (sql/format query-map)))

(defn f4 ^{:t :string} []
  (let [^{:t {:mmap [:string :any]}} query-map {:insert-into ^{:t {:mvector [:any]}} ["properties"]
                                                :columns     ^{:t {:mvector [:any]}} ["name" "surname" "age"]
                                                :values      ^{:t {:mvector [:any]}} [^{:t {:mvector [:any]}} ["'Jon'" "'Smith'" 34]
                                                                                      ^{:t {:mvector [:any]}} ["'Andrew'" "'Cooper'" 12]
                                                                                      ^{:t {:mvector [:any]}} ["'Jane'" "'Daniels'" 56]]}]
    (sql/format query-map)))

(defn -main ^{:t :void} [& _args]
  (let [^String query-str (f1)]
    (println (str "example 1 query string: [" query-str "]")))
  (let [^String query-str (f2)]
    (println (str "example 2 query string: [" query-str "]")))
  (let [^String query-str (f3)]
    (println (str "example 3 query string: [" query-str "]")))
  (let [^String query-str (f4)]
    (println (str "example 4 query string: [" query-str "]"))))
