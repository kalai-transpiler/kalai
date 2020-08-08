(ns kalai.placation
  (:require [expectations :as expectations]
            [clojure.string :as string]
            [clojure.test :refer [deftest is]]))

(defn divergence [s1 s2]
  (when (not= s1 s2)
    (loop [idx 0
           line 1
           column 1
           [c1 & s1'] s1
           [c2 & s2'] s2]
      (if (= c1 c2)
        (recur (inc idx)
               (if (= c1 \newline)
                 (inc line)
                 line)
               (if (= c1 \newline)
                 1
                 (inc column))
               s1'
               s2')
        [line column idx]))))

(defn pretty-str-diff [s1 s2]
  (let [[line column idx] (divergence s1 s2)
        lines1 (string/split-lines s1)
        lines2 (string/split-lines s2)
        line1 (nth lines1 (dec line))
        line2 (nth lines2 (dec line))
        start (dec (max 1 (- column 5)))
        end1 (min (count line1)
                  (+ start 30))
        end2 (min (count line2)
                  (+ start 30))

        d1 (subs line1 start end1)
        d2 (subs line2 start end2)
        spaces (apply str (repeat (- (dec column) start) " "))]
    (str \[ line \: column \] \newline
         d1 \newline
         d2 \newline
         spaces \^ \newline)))

(defmacro is= [x y]
  `(when (not= ~x ~y)
     (clojure.test/do-report
       (if (and (string? ~x) (string? ~y))
         {:type     :fail,
          :message  (pretty-str-diff ~x ~y)
          :expected '~'=
          :actual   '~'not=}
         (let [m# (expectations/compare-expr ~x ~y '~x '~y)]
           (assoc m#
             :type :fail
             :message (expectations/->failure-message m#)
             :expected '~'=
             :actual '~'not=))))))

#_(
   (deftest t
     (is= "hahaha\nhohoho\nhehehe"
          "hahaha\nhohoho\nhXhehe"))

   (deftest tt
     (is= '(atom 1)
          '(atom 2)))

   (deftest ttt
     (is= #{1 2}
          (conj #{1 3} 4))))
