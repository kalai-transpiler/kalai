(ns sql-builder.core-test
  (:require [sql-builder.core :as sql]
            [clojure.test :refer :all]))

(deftest readme-test
  (is (= (sql/format {:select ["foo.a" "foo.b" "foo.c"]
                      :from   ["foo"]
                      :where  ["AND"
                               ["=" "foo.a" "?a"]
                               ["=" "foo.b" "?b"]]})
         "SELECT foo.a, foo.b, foo.c FROM foo WHERE ((foo.a = ?a) AND (foo.b = ?b))")))

;; There are 3 forms of parameterization; named, ordinal, and numbered

;; create a parameterized query from data
;;Q -- allows reuse and labelling
{:select ["foo.a" "foo.b" "foo.c"]
 :from   ["foo"]
 :where  [:= "foo.a" "?x"]}
;; => SELECT foo.a, foo.b, foo.c FROM foo WHERE foo.a = ?x
;; (query Q {"?x" "baz"})
;; => results

;;P -- relies on order of parameters
{:select ["foo.a" "foo.b" "foo.c"]
 :from   ["foo"]
 :where  [:= "foo.a" "?"]}

;;M -- relies on order and allows reuse
{:select ["foo.a" "foo.b" "foo.c"]
 :from   ["foo"]
 :where  [:= "foo.a" "?1" "?1"]}

;; create a query
;; this is actually a bad pattern which we won't support,
;; we might have a separate interpolate function.
{:select ["foo.a" "foo.b" "foo.c"]
 :from   ["foo"]
 :where  [:= "foo.a" "baz"]}
