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

(def rewrite
  (s/bottom-up
    (s/rewrite
      (j/invoke (u/var ~#'println) & ?more)
      (j/invoke System.out.println & ?more)

      ;; TODO: put a predicate to ensure ?coll is not a seq because Rust .iter()
      ;; is not allowed/available on a Rust Iterator
      (j/invoke (u/var ~#'seq) ?coll)
      (j/method stream ?coll)

      (j/invoke (u/var ~#'first) ?seq)
      (j/method get (j/method findFirst ?seq))

      (j/invoke (u/var ~#'next) ?seq)
      (j/method skip ?seq 1)


      ;; TODO: these should be (u/var)
      (j/invoke clojure.lang.RT/count ?x)
      (j/method (m/app count-for ?x) ?x)

      ;; TODO: need to do different stuff depending on the type
      (j/invoke clojure.lang.RT/nth ?x ?n)
      (j/method (m/app nth-for ?x) ?x ?n)

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

      (j/invoke (u/var ~#'conj) & ?more)
      (j/method add & ?more)

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
      (j/invoke String.join "" ?col)

      (j/invoke (u/var ~#'str/join) ?sep ?col)
      (j/invoke String.join ?sep ?col)

      (j/invoke (u/var ~#'map) ?fn ?xs)
      (j/method collect
                (j/method map ?xs ~(symbol (ju/fully-qualified-function-identifier-str ?fn "::")))
                (j/invoke Collectors.toList))

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
