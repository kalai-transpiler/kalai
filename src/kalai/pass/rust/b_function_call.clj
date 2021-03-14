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
      ;; TODO: consolidate the <string>.length(), <StringBuffer>.length(), and
      ;; clojure.lang.RT/count(<string>) rules into one rule
      (r/method length (u/of-t :string ?this))
      (r/method unwrap (r/method try_into (r/method count (r/method chars ?this))))

      (r/method length (u/of-t StringBuffer ?this))
      (r/method unwrap (r/method try_into (r/method count (r/method chars ?this))))


      (r/method size ?this)
      (r/method unwrap (r/method try_into (r/method len ?this)))

      (r/new ?sym)
      (r/new ~(or (get-in types/lang-type-mappings [:kalai.emit.langs/rust (:t (meta ?sym))])
                  ?sym))

      (r/method append (u/of-t StringBuffer ?this) ?x)
      (r/method push_str ?this ?x)

      (r/method toString (u/of-t StringBuffer ?this))
      ?this


      (r/method insert (u/of-t StringBuffer ?this) ?idx (u/of-t :char ?s2))
      (r/method insert ?this (r/cast ?idx :usize)
        ~(if (:ref (meta ?s2))
           (list 'r/deref ?s2)
           ?s2))

      (r/method insert (u/of-t StringBuffer ?this) ?idx ?s2)
      (r/method insert_str ?this (r/cast ?idx :usize) (r/ref (r/method to_string ?s2)))


      ;; TODO: these should be (u/var)
      (r/invoke clojure.lang.RT/count (u/of-t :string ?x))
      (r/method unwrap (r/method try_into (r/method count (r/method chars ?x))))

      (r/invoke clojure.lang.RT/count ?x)
      (r/method unwrap (r/method try_into (r/method len ?x)))


      (r/invoke clojure.lang.RT/nth (u/of-t :string ?x) ?n)
      (r/method unwrap (r/method nth (r/method chars ?x) (r/cast ?n :usize)))

      (r/invoke clojure.lang.RT/nth ?x ?n)
      (r/deref (r/method unwrap (r/method get ?x (r/cast ?n :usize))))


      ;; TODO: for vectors, we should detect the vector type and do a
      ;; cast of the index argument to `usize` like we do for `nth`
      (r/invoke clojure.lang.RT/get ?x ?k)
      (r/method unwrap (r/method get ?x (r/ref ?k)))

      (r/invoke (u/var ~#'contains?) ?coll ?x)
      (r/method contains_key ?coll (r/ref ?x))

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
