(ns clj-icu-test.emit.interface)

;;
;; dispatch fn(s)
;;

(defn lang
  [ast-opts]
  (:lang ast-opts))

;;
;; multimethod specs
;;

(defmulti emit-const lang)
