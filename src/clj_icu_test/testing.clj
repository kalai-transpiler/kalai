(ns clj-icu-test.testing)

(defn new-name-testing-fn
  "Provides a testing version of curlybrace-util/new-name that is suitable for shadowing the real fn in tests b/c it has deterministic behavior.  More specifically, the user is required to provide the suffix on the name.  Shadowing can be achieved by using with-redefs. Use this fn with partial to match the original arity of curlybrace-util/new-name."
  [suffix name]
  (-> (str name suffix)))
