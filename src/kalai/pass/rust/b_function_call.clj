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
    {(m/pred #{:map :set}) (m/pred some?)} size
    ?else len))

;; Note: ifn? would be more permissive, but it would include using data structures as functions
;; which would require more syntactic gymnastics to translate into each target language
(defn fn-var?
  "Indicates whether a value in the S-expressions (emitted by tools.analyzer) is a function
  var. Examples include `inc`, `assoc`, or any previously-defined user functions."
  [x]
  (some-> x meta :var deref fn?))

(declare rewrite)

(defn maybe-lambda
  "For HOFs, transpiling a user-provided function literal works fine. But when the user
  provides a function var (ex: `inc`, `assoc`), the target language does not necessarily
  handle the output (ex: because it needs to know which arity of the function), so we
  always create our own lambda in such cases."
  [?fn arg-count]
  (if (fn-var? ?fn)
    (let [args (mapv symbol (map str (take arg-count "abcdefghikjlmnopqrstuvwxyz")))]
      (list 'r/lambda
            args
            (list 'r/block
                  (rewrite (list* (if (u/operator-symbols ?fn)
                                    'r/operator
                                    'r/invoke)
                                  ?fn
                                  args)))))
    ?fn))

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

      (r/invoke clojure.lang.RT/count ?x)
      (r/cast (r/method ~(count-for ?x) ?x) :int)

      (r/invoke clojure.lang.RT/nth (u/of-t :string ?x) ?n)
      (r/method unwrap (r/method nth (r/method chars ?x) (r/cast ?n :usize)))

      (r/invoke clojure.lang.RT/nth ?x ?n)
      (r/method clone (r/method unwrap (r/method get ?x (r/cast ?n :usize))))

      (m/and
        (r/invoke clojure.lang.RT/nth ?x ?n ?not-found)
        ;; Note: not using `u/tmp-for` because we don't want to create a type
        ;; for the temporary variable because the type will be a Rust `Some<T>`
        ;; type, which as a Rust-specific type, we cannot/do not want to express in Kalai.
        (m/let [?get (u/gensym2 "get")]))
      (r/block
        (r/init ?get (r/method get ?x (r/cast ?n :usize)))
        (r/if (r/method is_some ?get)
          (r/block (r/method clone (r/method unwrap ?get)))
          (r/block ?not-found)))

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

      (r/invoke (u/var ~#'disj) & ?more)
      (r/method remove & ?more)

      ;; conj - vectors and sets
      (r/invoke (u/var ~#'conj)
                (m/and ?coll
                       (m/app meta {:t {_ [?value-t]}}))
                . !arg ...)
      (r/method push ?coll . (m/app #(ru/wrap-value-enum ?value-t %) !arg) ...)

      ;; conj - maps
      (m/and
        (r/invoke (u/var ~#'conj)
                  (m/and ?coll
                         (m/app meta {:t {:mmap [?key-t ?value-t]
                                          :as ?t}}))
                  . (m/and !arg (m/app meta {:t {_ [?key-t ?value-t]}})) ...)
        ?expr
        (m/let [?tmp (u/tmp ?t ?expr)]))
      (m/app
        #(u/preserve-type ?expr %)
        (r/block
          (r/init ?tmp ?coll)
          (r/expression-statement (r/method extend ?tmp . !arg ...))
          ?tmp))

      ;;;; conj - persistent map
      ;;(m/and
      ;;  (r/invoke (u/var ~#'conj)
      ;;            (m/and ?coll
      ;;                   (m/app meta {:t {:map [?key-t ?value-t]
      ;;                                    :as ?t}}))
      ;;            . (m/and !arg (m/app meta {:t {_ [?key-t ?value-t]}})) ...)
      ;;  ?expr
      ;;  (m/let [?tmp (u/tmp ?t ?expr)]))
      ;;(m/app
      ;;  #(u/preserve-type ?expr %)
      ;;  (r/block
      ;;    (r/init ?tmp (r/method clone ?coll))
      ;;    ;; map.iter().foreach(|tuple| tmp.insert_mut(tuple.0, tuple.1))
      ;;    . (r/expression-statement (r/method for_each
      ;;                                        (r/method iter !arg)
      ;;                                        (r/lambda [tuple]
      ;;                                                  (r/method insert_mut ?tmp
      ;;                                                            (r/method clone (r/field 0 tuple))
      ;;                                                            (r/method clone (r/field 1 tuple)))))) ...
      ;;    ?tmp))

      ;; When inc is used as a function value for example (update m :x inc)
      ;; See kalai/operators for when directly called
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

      (r/invoke (u/var ~#'doall) ?xs)
      ?xs

      (r/invoke (u/var ~#'map) ?fn ?xs)
      (r/method map
                (r/method into_iter (r/method clone ?xs))
                ~(maybe-lambda ?fn 1))

      (r/invoke (u/var ~#'map) ?fn ?xs ?ys)
      (r/method map
                (r/invoke "std::iter::zip" ?xs ?ys)
                (r/lambda [t] (r/block (r/invoke ~(maybe-lambda ?fn 2)
                                                 (r/field 0 t)
                                                 (r/field 1 t)))))

      (r/invoke (u/var ~#'reduce) ?fn ?xs)
      (r/method unwrap
                (r/method reduce
                          (r/method into_iter (r/method clone ?xs))
                          ~(maybe-lambda ?fn 2)))

      (r/invoke (u/var ~#'reduce) ?fn ?initial ?xs)
      (r/method fold
                (r/method into_iter (r/method clone ?xs))
                ?initial
                ~(maybe-lambda ?fn 2))

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
