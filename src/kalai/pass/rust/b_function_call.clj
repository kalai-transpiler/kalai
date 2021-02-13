(ns kalai.pass.rust.b-function-call
  (:require [kalai.util :as u]
            [meander.strategy.epsilon :as s]
            [meander.epsilon :as m]
            [clojure.string :as str]
            [kalai.types :as types]))

;; TODO: user extension point, is dynamic var good?
;; can it be more data driven?
(def ^:dynamic *user*)

(defn count-for [x]
  (m/rewrite (:t (meta x))
    {(m/pred #{:mmap :map :mset :set :mvector :vector}) (m/pred some?)} 'len
    ?else 'len))

(def rewrite
  (s/bottom-up
    (s/rewrite
      (r/invoke (u/var ~#'println) & ?args)
      (r/invoke println!
                (r/literal ~(->> (repeat (count ?args) "{}")
                                 (str/join " ")))
                & ?args)

      ;; Remember that ^{:t java.lang.String} gets converted to ^{:t :string} upstream
      ;; (AST rewriting), whereas other Java class/types are left as-is in the metadata
      ;; map.
      (r/method length (u/of-t :string ?this))
      (r/method unwrap (r/method try_into (r/method len ?this)))

      (r/method length (u/of-t StringBuffer ?this))
      (r/method unwrap (r/method try_into (r/method len ?this)))

      (r/method size ?this)
      (r/method unwrap (r/method try_into (r/method len ?this)))

      (r/new ?sym)
      (r/new ~(or (get-in types/lang-type-mappings [:kalai.emit.langs/rust (:t (meta ?sym))])
                  ?sym))

      (r/method append (u/of-t StringBuffer ?this) ?x)
      (r/method push_str ?this ?x)

      (r/method toString (u/of-t StringBuffer ?this))
      ?this

      #_#_(r/method insert (u/of-t StringBuffer ?this) ?idx ?s2)
      (r/block
        (m/let [t (u/tmp StringBuffer)]
               (r/assign t ?this)
               (r/invoke truncate t ?idx)
               (r/invoke push_str t ?s2)))

      ;; TODO: these should be (u/var)
      (r/invoke clojure.lang.RT/count ?x)
      (r/method unwrap (r/method try_into (r/method len ?x)))

      ;; TODO: need to do different stuff depending on the type
      (r/invoke clojure.lang.RT/nth ?x ?n)
      (r/method unwrap (r/method get ?x ?n))

      (r/invoke clojure.lang.RT/get ?x ?k)
      (r/method get ?x (r/ref ?k))

      (r/invoke (u/var ~#'assoc) & ?more)
      (r/method insert & ?more)

      (r/invoke (u/var ~#'dissoc) & ?more)
      (r/method remove & ?more)

      (r/invoke (u/var ~#'conj) & ?more)
      (r/method push & ?more)

      (r/invoke (u/var ~#'inc) ?x)
      (r/operator + ?x 1)

      (r/invoke (u/var ~#'update) ?x ?k ?f & ?args)
      (r/method insert ?x ?k
                (m/app rewrite (r/invoke ?f (r/method unwrap (r/invoke clojure.lang.RT/get ?x ?k)) & ?args)))

      ?else
      ?else)))
