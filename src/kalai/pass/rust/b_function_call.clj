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
      (r/cast (r/method count (r/method chars ?this)) :int)

      ;; TODO: do we support the Clojure casting functions `int`, `float`, etc. to
      ;; give users control on more precise types?
      (r/method length (u/of-t StringBuffer ?this))
      (r/cast (r/method len ?this) :int)


      (r/method size ?this)
      (r/cast (r/method len ?this) :int)

      (r/new ?sym)
      (r/new ~(or (get-in types/lang-type-mappings [:kalai.emit.langs/rust (:t (meta ?sym))])
                  ?sym))

      (r/method append (u/of-t StringBuffer ?this) ?x)
      (r/method push_str ?this ?x)

      (r/method toString (u/of-t StringBuffer ?this))
      (r/method collect (r/method iter ?this))


      (r/method insert (u/of-t StringBuffer ?this) ?idx (u/of-t :char ?s2))
      (r/method insert ?this (r/cast ?idx :usize)
        ~(if (:ref (meta ?s2))
           (list 'r/deref ?s2)
           ?s2))

      (r/method insert (u/of-t StringBuffer ?this) ?idx ?s2)
      (r/method splice ?this (r/range ?idx ?idx) (r/method ^{:t {:mvector [:char]}} collect (r/method chars (r/method to_string ?s2))))


      (r/invoke java.lang.System/getenv ?x)
      (r/method unwrap (r/invoke "std::env::var" ?x))

      ;; TODO: these should be (u/var)
      (r/invoke clojure.lang.RT/count (u/of-t :string ?x))
      (r/cast (r/method count (r/method chars ?x)) :int)

      (r/invoke clojure.lang.RT/count ?x)
      (r/cast (r/method len ?x) :int)


      (r/invoke clojure.lang.RT/nth (u/of-t :string ?x) ?n)
      (r/method unwrap (r/method nth (r/method chars ?x) (r/cast ?n :usize)))

      (r/invoke clojure.lang.RT/nth ?x ?n)
      (r/method clone (r/method unwrap (r/method get ?x (r/cast ?n :usize))))


      ;; TODO: for vectors, we should detect the vector type and do a
      ;; cast of the index argument to `usize` like we do for `nth`
      (r/invoke clojure.lang.RT/get ?x ?k)
      (r/method clone (r/method unwrap (r/method get ?x (r/ref ?k))))

      (r/invoke (u/var ~#'contains?) ?coll ?x)
      (r/method contains_key ?coll (r/ref ?x))

      (r/invoke (u/var ~#'assoc) ?coll . !arg ...)
      (r/method insert ?coll . (r/method clone !arg) ...)

      (r/invoke (u/var ~#'dissoc) & ?more)
      (r/method remove & ?more)

      (r/invoke (u/var ~#'conj) ?coll . !arg ...)
      (r/method push ?coll . (r/method clone !arg) ...)

      (r/invoke (u/var ~#'inc) ?x)
      (r/operator + ?x 1)

      (r/invoke (u/var ~#'update) ?x ?k ?f & ?args)
      (r/method insert ?x (r/method clone ?k)
                (m/app rewrite (r/invoke ?f (r/invoke clojure.lang.RT/get ?x ?k) & ?args)))

      ?else
      ?else)))
