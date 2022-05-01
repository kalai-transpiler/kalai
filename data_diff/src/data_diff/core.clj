(ns data-diff.core)

(declare diff)

(defn diff-associative-key
  "Diff associative things a and b, comparing only the key k."
  [a b k]
  (let [va (get a k)
        vb (get b k)
        [a* b* ab] (diff va vb)
        in-a (contains? a k)
        in-b (contains? b k)
        same (and in-a in-b
                  (or (not (nil? ab))
                      (and (nil? va) (nil? vb))))]
    [(when (and in-a (or (not (nil? a*)) (not same))) {k a*})
     (when (and in-b (or (not (nil? b*)) (not same))) {k b*})
     (when same {k ab})
     ]))

(defn diff-associative
  "Diff associative things a and b, comparing only keys in ks."
  [a b ks]
  (reduce
    (fn [diff1 diff2]
      (doall (map merge diff1 diff2)))
    [nil nil nil]
    (map
      (partial diff-associative-key a b)
      ks)))

;;(defn- diff-sequential
;;  [a b]
;;  (vec (map vectorize (diff-associative
;;                        (if (vector? a) a (vec a))
;;                        (if (vector? b) b (vec b))
;;                        (range (max (count a) (count b)))))))

;;(equality-partition [^Object x]
;;  (if (.. x getClass isArray) :sequential :atom))

(defn union
  [s1 s2]
  (if (< (count s1) (count s2))
    (reduce conj s2 s1)
    (reduce conj s1 s2)))

(defn diff
  "Recursively compares a and b, returning a tuple of
  [things-only-in-a things-only-in-b things-in-both].
  Comparison rules:

  * For equal a and b, return [nil nil a].
  * Maps are subdiffed where keys match and values differ.
  * Sets are never subdiffed.
  * All sequential things are treated as associative collections
    by their indexes, with results returned as vectors.
  * Everything else (including strings!) is treated as
    an atom and compared for equality."
  {:added "1.3"}
  [a b]
  (if (= a b)
    [nil nil a]
    (let [ab-keys (union (keys a) (keys b))]
      (diff-associative a b ab-keys))
    ;;(if (= (equality-partition a) (equality-partition b))
    ;;  (diff-similar a b)
    ;;  (atom-diff a b))
    ))