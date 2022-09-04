(ns kalai.pass.java.b-function-call
  (:require [kalai.util :as u]
            [kalai.pass.java.util :as ju]
            [meander.strategy.epsilon :as s]
            [meander.epsilon :as m]
            [clojure.string :as str]
            [camel-snake-kebab.core :as csk]))

;; TODO: user extension point, is dynamic var good?
;; can it be more data driven?
(def ^:dynamic *user*)

;; If we do this before syntax, we can remove j/invoke... is that good or bad?
;; (do we match on the Java syntax, or the Kalai syntax?)

;; Java method name to use for Clojure `nth`, based on collection type it was called on
(defn nth-for [x]
  (if (= (:t (meta x)) :string)
    'charAt
    'get))

(defn count-for [x]
  (m/rewrite (:t (meta x))
    {(m/pred #{:mmap :map :mset :set :mvector :vector}) (m/pred some?)} 'size
    ?else 'length))

(defn class-name [sym]
  (when-let [s (some-> sym meta :var meta :ns str)]
    (let [xs (str/split s #"\.")
          packagename (str/join "." (for [z (butlast xs)]
                                      (str/lower-case (csk/->camelCase z))))
          classname (csk/->PascalCase (last xs))]
      (str packagename "." classname))))

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
      (list 'j/lambda
            args
            (list 'j/block
                  (list 'j/expression-statement
                        (list 'j/return
                              (rewrite (list* (if (u/operator-symbols ?fn)
                                                'j/operator
                                                'j/invoke)
                                              ?fn
                                              args)))))))
    ?fn))

(def rewrite
  (s/bottom-up
    (s/rewrite
      (j/invoke (u/var ~#'println) . !more ..2)
      (j/invoke System.out.println (j/operator + "" . !more ...))

      (j/invoke (u/var ~#'println) & ?more)
      (j/invoke System.out.println & ?more)

      ;; we add `{:seq true}` to metadata to enable checking downstream whether ?coll is a seq because Java .stream()
      ;; is not allowed/available on a Java Stream
      (j/invoke (u/var ~#'seq) ?coll)
      (m/app #(with-meta % {:seq true}) (j/method stream ?coll))

      (j/invoke (u/var ~#'first) ?seq)
      (j/method get (j/method findFirst ?seq))

      ;; we add `{:seq true}` to metadata to enable checking downstream whether ?coll is a seq because Java .stream()
      ;; is not allowed/available on a Java Stream
      (j/invoke (u/var ~#'next) ?seq)
      (m/app #(with-meta % {:seq true}) (j/method skip ?seq 1))

      ;; TODO: these should be (u/var)
      (j/invoke clojure.lang.RT/count ?x)
      (j/method (m/app count-for ?x) ?x)

      ;; TODO: need to do different stuff depending on the type
      (j/invoke clojure.lang.RT/nth ?x ?n)
      (j/method (m/app nth-for ?x) ?x ?n)

      (m/and
        (j/invoke clojure.lang.RT/nth ?x ?n ?not-found)
        ;; Note: not using `u/tmp-for` because we don't want to create a type
        ;; for the temporary variable because the type will be a Rust `Some<T>`
        ;; type, which as a Rust-specific type, we cannot/do not want to express in Kalai.
        ;; TODO (if needed): use the collection (?x)'s element type instead of hard-coding
        ;; `:any` as the type of the temp/result variable. This could be done for collections
        ;; (and check for the special case of strings, where elem is a char), but for a sequence,
        ;; (in Java, a Stream), we only annotate them internally and never expose to the user,
        ;; and in those cases the `:seqeunce:` type would need to more precisely specify the
        ;; element type of the sequence.
        (m/let [?result (u/tmp :any ?not-found)]))
      (group
        (j/init ?result ?not-found)
        (j/if (j/operator <= 0 ?n)
          (j/if (j/operator < ?n (j/method (m/app count-for ?x) ?x))
            (j/block (j/assign ?result (j/method (m/app nth-for ?x) ?x)))))
        ?result)

      ;; special case how persistent maps (via Bifurcan) do .get(key) so that we _don't_ return an Optional<value>
      (j/invoke clojure.lang.RT/get
                (m/and ?x (m/pred (comp :map :t meta)))
                ?k)
      (j/method get ?x ?k nil)

      ;; default case for .get(key) with no default value
      (j/invoke clojure.lang.RT/get ?x ?k)
      (j/method get ?x ?k)

      ;; TODO: this only works on Maps, is there an equivalent for Lists (vectors) and Sets?
      (j/invoke clojure.lang.RT/get ?x ?k ?default)
      (j/method getOrDefault ?x ?k ?default)

      (j/invoke (u/var ~#'contains?) ?coll ?x)
      (j/method containsKey ?coll ?x)

      (j/operator ==
                  (m/and (m/or (m/pred string?) (m/app meta {:t :string})) ?x)
                  (m/and (m/or (m/pred string?) (m/app meta {:t :string})) ?y))
      (j/method equals ?x ?y)

      (j/invoke (u/var ~#'assoc) & ?more)
      (j/method put & ?more)

      (j/invoke (u/var ~#'dissoc) & ?more)
      (j/method remove & ?more)

      ;; vectors and sets
      ;;TODO: fix variadic or don't support it for assoc and conj
      (j/invoke (u/var ~#'conj)
                (m/and ?coll
                       (m/app meta {:t {_ [?value-t]}}))
                . !arg ...)
      (j/method add . !arg ...)

      ;; maps
      ;;TODO: fix variadic or don't support it for assoc and conj
      (j/invoke (u/var ~#'conj)
                (m/and ?coll
                       ;; TODO: fix for persistent map
                       (m/app meta {:t {:mmap [?key-t ?value-t]
                                        :as   ?t}}))
                . (m/and !arg (m/app meta {:t {_ [?key-t ?value-t]}})) ...)
      (j/invoke kalai.Kalai.conj ?coll . !arg ...)

      (j/invoke (u/var ~#'inc) ?x)
      (j/operator + ?x 1)

      (j/invoke (u/var ~#'update) ?x ?k ?f & ?args)
      (j/method put ?x ?k
                (m/app rewrite (j/invoke ?f (j/method get ?x ?k) & ?args)))

      (j/invoke clojure.lang.Util/identical ?x nil)
      (j/operator == ?x nil)

      (j/invoke (u/var ~#'vector?) ?x)
      (j/operator instanceof ?x List)

      (j/invoke (u/var ~#'set?) ?x)
      (j/operator instanceof ?x Set)

      (j/invoke (u/var ~#'map?) ?x)
      (j/operator instanceof ?x Map)

      (j/invoke (u/var ~#'string?) ?x)
      (j/operator instanceof ?x String)

      (j/invoke clojure.core/instance? ~Integer ?x)
      (j/operator instanceof ?x Integer)

      (j/invoke clojure.core/instance? ~Long ?x)
      (j/operator instanceof ?x Long)

      (j/invoke clojure.core/instance? ~Byte ?x)
      (j/operator instanceof ?x Byte)

      (j/invoke (u/var ~#'boolean?) ?x)
      (j/operator instanceof ?x Boolean)

      (j/invoke (u/var ~#'double) ?x)
      (j/operator instanceof ?x Double)

      (j/invoke (u/var ~#'float) ?x)
      (j/operator instanceof ?x Float)

      (j/invoke (u/var ~#'str/join) ?col)
      (j/invoke String.join "" (j/method collect ?col (j/invoke Collectors.toList)))

      (j/invoke (u/var ~#'str/join) ?sep ?col)
      (j/invoke String.join ?sep (j/method collect ?col (j/invoke Collectors.toList)))

      (j/invoke (u/var ~#'map) ?fn ?xs)
      (m/app #(with-meta % {:seq true})
             ;; TODO: is there a cleaner version
             (j/method map ~(if (:seq (meta ?xs))
                              ?xs
                              (list 'j/method 'stream ?xs))
                       ~(maybe-lambda ?fn 1)))

      (j/invoke (u/var ~#'map) ?fn ?xs ?ys)
      (m/app #(with-meta % {:seq true})
             ;; TODO: is there a cleaner version
             (j/invoke kalai.Kalai.map ~(maybe-lambda ?fn 2)
                       ~(if (:seq (meta ?xs))
                          ?xs
                          (list 'j/method 'stream ?xs))
                       ~(if (:seq (meta ?ys))
                          ?ys
                          (list 'j/method 'stream ?ys))))

      (j/invoke (u/var ~#'reduce) ?fn ?xs)
      (j/method get
                (j/method reduce
                          ~(if (:seq (meta ?xs))
                             ?xs
                             (list 'j/method 'stream ?xs))
                          ~(maybe-lambda ?fn 2)))

      ;; Options:
      ;; 1. https://stackoverflow.com/a/67532404
      ;; Collectors.collectingAndThen(
      ;;            Collectors.reducing(Function.<B>identity(), a -> b -> f.apply(b, a), Function::andThen),
      ;;            endo -> endo.apply(init)
      ;;    )
      ;; 2. a lambda that does this https://stackoverflow.com/a/53499752
      ;; <U, T> U foldLeft(Collection<T> sequence, U identity, BiFunction<U, ? super T, U> accumulator) {
      ;;    U result = identity;
      ;;    for (T element : sequence)
      ;;        result = accumulator.apply(result, element);
      ;;    return result;
      ;;}
      ;; TODO: use gensym
      (j/invoke (u/var ~#'reduce) ?fn ?initial ?xs)
      (j/invoke kalai.Kalai.foldLeft ?xs ?initial ?fn)

      (j/invoke (u/var ~#'str) & ?args)
      (j/operator + "" & ?args)

      ;; Keep this below any match by symbol rules!
      (j/invoke (m/and (m/pred symbol?)
                       (m/pred #(not (:var (meta %))))
                       (m/pred #(str/includes? (str %) "/"))
                       ?static-function)
                & ?more)
      (j/invoke ~(-> (str ?static-function)
                     (str/replace "/" ".")
                     (symbol))
                & ?more)

      ?else
      ?else)))
