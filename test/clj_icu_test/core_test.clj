(ns clj-icu-test.core-test
  (:require ;;[clojure.test :refer :all]
            [clojure.tools.analyzer.jvm :as az]
            [clj-icu-test.core :refer :all]
            [expectations :refer :all])
  (:import clj_icu_test.core.AstOpts))

;;
;; Note: Java and C++ unit tests have moved to java_test.clj and cpp_test.clj
;;

;; fn-matches?

(let [ast (az/analyze '(println "Yaarrrrgh!"))
      fn-meta-ast (-> ast :fn :meta)]
  ;; positive case
  (expect (fn-matches? fn-meta-ast "clojure.core" "println"))
  ;; negative case
  (expect false? (fn-matches? fn-meta-ast "clojure.core.subnamespace" "println"))
  (expect false? (fn-matches? fn-meta-ast "clojure.core" "some-other-fn"))
  )

;; instance-call-matches?

(let [ast (az/analyze '(let [sb (StringBuffer.)]
                         (.appendMe sb "elango")))
      inst-call-ast (-> ast :body)]
  ;; positive case
  (expect (instance-call-matches? {:inst-call-ast inst-call-ast
                                   :exp-instance-class StringBuffer
                                   :exp-method-name "appendMe"}))
  ;; negative case
  (expect false? (instance-call-matches? {:inst-call-ast inst-call-ast
                                          :exp-instance-class StringBuffer
                                          :exp-method-name "appendMeOkurrr"})))
