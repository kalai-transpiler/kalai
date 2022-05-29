(ns kalai.pass.rust.b-function-call
  (:require [kalai.pass.rust.util :as ru]
            [kalai.util :as u]
            [meander.strategy.epsilon :as s]
            [meander.epsilon :as m]
            [clojure.string :as str]
            [kalai.types :as types]
            [clojure.string :as string]))

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

      ;; In Rust, we don't need to insert `{:seq true}` in metadata because `.into_iter()` is idempotent on Rust Iterators
      (r/invoke (u/var ~#'seq) ?coll)
      (r/method into_iter (r/method clone ?coll))

      (r/invoke (u/var ~#'first) ?seq)
      (r/method clone (r/method unwrap (r/method next ?seq)))

      (r/invoke (u/var ~#'next) ?seq)
      (r/method skip ?seq (r/literal 1))

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
      (r/method collect (r/method into_iter ?this))

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

      (r/invoke clojure.lang.RT/count
                (m/and ?x
                       (m/app (comp :t meta) (m/and ?t
                                                    (m/or (m/pred :set)
                                                          (m/pred :map))))))
      (r/cast (r/method size ?x) :int)

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

      (r/invoke clojure.lang.RT/get ?x ?k ?default)
      (r/method clone (r/method unwrap_or (r/method get ?x (r/ref ?k)) (r/ref ?default)))

      (r/invoke (u/var ~#'contains?) ?coll ?x)
      (r/method contains_key ?coll (r/ref ?x))

      (r/invoke (u/var ~#'assoc)
                (m/and ?coll
                       (m/app meta {:t {_ [?key-t ?value-t]}}))
                . !key !value ...)
      (r/method insert ?coll
                .
                (m/app #(ru/wrap-value-enum :int %) !key)
                (m/app #(ru/wrap-value-enum :float %) !value)
                ...)

      (r/invoke (u/var ~#'dissoc) & ?more)
      (r/method remove & ?more)

      ;; Note: this particular rule would only support vectors and sets (maps would need to be handled differently)
      (r/invoke (u/var ~#'conj)
                (m/and ?coll
                       (m/app meta {:t {_ [?value-t]}}))
                . !arg ...)
      (r/method push ?coll . (m/app #(ru/wrap-value-enum ?value-t %) !arg) ...)

      ;; When inc is used as a function value for example (update m :x inc)
      ;; See kalai/operators for when direcly called
      (r/invoke (u/var ~#'inc) ?x)
      (r/operator + ?x 1)

      (r/invoke (u/var ~#'update) ?x ?k ?f & ?args)
      (r/method insert ?x (r/method clone ?k)
                (m/app rewrite (r/invoke ?f (r/invoke clojure.lang.RT/get ?x ?k) & ?args)))

      (r/invoke (u/var ~#'str) & ?args)
      (r/invoke format! (r/literal ~(str/join (repeat (count ?args) "{}"))) & ?args)

      ;; Assuming that ?xs are strings, for now
      (r/invoke (u/var ~#'str/join) ?xs)
      (r/method join ?xs)

      ;; Assuming that ?xs are strings, and ?sep is a string, for now
      (r/invoke (u/var ~#'str/join) ?sep ?xs)
      (r/method join
                (r/method "collect::<Vec<String>>" ?xs)
                (r/ref ?sep))

      (r/invoke (u/var ~#'map) ?fn ?xs)
      (r/method map
                (r/method into_iter (r/method clone ?xs))
                ;; TODO: maybe gensym the argname
                ?fn)

      ;; TODO: do we really need to clone here???
      (r/invoke (u/var ~#'vector?) ?x)
      (r/operator ||
                  (r/method is_type ?x (r/literal "Vector"))
                  (r/method is_type ?x (r/literal "Vec")))

      (r/invoke (u/var ~#'set?) ?x)
      (r/method is_type ?x (r/literal "Set"))

      (r/invoke (u/var ~#'map?) ?x)
      (r/operator ||
                  (r/method is_type ?x (r/literal "Map"))
                  (r/method is_type ?x (r/literal "PMap")))

      (r/invoke (u/var ~#'string?) ?x)
      (r/method is_type ?x (r/literal "String"))

      (r/invoke clojure.core/instance? ~Integer ?x)
      (r/method is_type ?x (r/literal "i32"))

      (r/invoke clojure.core/instance? ~Long ?x)
      (r/method is_type ?x (r/literal "i64"))

      (r/invoke clojure.core/instance? ~Byte ?x)
      (r/method is_type ?x (r/literal "u8"))

      (r/invoke (u/var ~#'boolean?) ?x)
      (r/method is_type ?x (r/literal "bool"))

      (r/invoke (u/var ~#'double) ?x)
      (r/method is_type ?x (r/literal "Double"))

      (r/invoke (u/var ~#'float) ?x)
      (r/method is_type ?x (r/literal "Float"))

      (r/invoke clojure.lang.Util/identical ?x nil)
      (r/method is_type ?x (r/literal "Nil"))

      ?else
      ?else)))
