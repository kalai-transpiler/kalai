(ns kalai.pass.rust.b-function-call
  (:require [kalai.util :as u]
            [meander.strategy.epsilon :as s]
            [meander.epsilon :as m]
            [clojure.string :as str]))

;; TODO: user extension point, is dynamic var good?
;; can it be more data driven?
(def ^:dynamic *user*)

(defn nth-for [x]
  (if (= (:t (meta x)) :string)
    'charAt
    'get))

(defn count-for [x]
  (m/rewrite (:t (meta x))
    {(m/pred #{:mmap :map :mset :set :mvector :vector}) (m/pred some?)} 'size
    ?else 'length))

(def rewrite
  (s/bottom-up
    (s/rewrite
      (r/invoke (u/var ~#'println) & ?args)
      (r/invoke println!
                ~(->> (repeat (count ?args) "{}")
                      (str/join " "))
                & ?args)

      (r/construct StringBuffer)
      (r/construct String)

      (r/method append (u/of-tag StringBuffer ?this) ?x)
      (r/method push_str ?this ?x)

      (r/method length (u/of-tag StringBuffer ?this))
      (r/method len ?this)

      (r/method toString (u/of-tag StringBuffer ?this))
      ?this

      #_#_(r/method insert (u/of-tag StringBuffer ?this) ?idx ?s2)
      (r/block
        (m/let [t (u/tmp StringBuffer)]
               (r/assign t ?this)
               (r/invoke truncate t ?idx)
               (r/invoke push_str t ?s2)))

      (r/invoke clojure.lang.RT/count ?x)
      (r/method (m/app count-for ?x) ?x)

      ;; TODO: need to do different stuff depending on the type
      (r/invoke clojure.lang.RT/nth ?x ?n)
      (r/method (m/app nth-for ?x) ?x ?n)

      (r/invoke clojure.lang.RT/get ?x ?k)
      (r/method get ?x ?k)

      (r/operator ==
        (m/and (m/or (m/pred string?) (m/app meta {:t :string})) ?x)
        (m/and (m/or (m/pred string?) (m/app meta {:t :string})) ?y))
      (r/method equals ?x ?y)

      ?else
      ?else)))
