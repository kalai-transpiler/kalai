(ns sql-builder.core-test
  (:require [sql-builder.core :as sql]
            [clojure.test :refer :all]))

(deftest foo
  (is (= 1 1)))

(deftest readme-test
  (is (= (sql/format {:select [:foo/a :foo/b :foo/c]
                      :from   [:foo]
                      :where  [:= :foo/a "baz"]})
         ["SELECT foo.a, foo.b, foo.c FROM foo WHERE foo.a = ?" "baz"])))