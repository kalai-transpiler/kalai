(ns data-diff.core)

(declare diff)

(defn diff-associative-key
  "Diff associative things a and b, comparing only the key k."
  [a b k]
  (let [va (get a k)
        vb (get b k)
        [aa bb ab] (diff va vb)
        in-a (contains? a k)
        in-b (contains? b k)
        same (and in-a in-b
                  (or (not (nil? ab))
                      (and (nil? va) (nil? vb))))]
    ;; TODO: this produces weird nonsense `if` when p and q are inline
    ;; TODO: create a simpler test that recreates by having a boolean expression in an if or when block
    [(when (and in-a (or (not (nil? aa)) (not same))) {k aa})
     (when (and in-b (or (not (nil? bb)) (not same))) {k bb})
     (when same {k ab})]))

(defn merge2
  "A helper function to replace `merge` with `(reduce conj...)`"
  [m1 m2]
  (reduce conj m1 m2))

(defn diff-associative
  "Diff associative things a and b, comparing only keys in ks."
  [a b ks]
  (reduce
    (fn [diff1 diff2]
      ;; TODO: move the explict seq calls into function_call
      (doall (map merge2 (seq diff1) (seq diff2))))
    [nil nil nil]
    (map (fn [k]
           (diff-associative-key a b k))
         (seq ks))))

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

(defn difference
  "Return a set that is the first set without elements of the remaining sets"
  [s1 s2]
  (if (< (count s1) (count s2))
    (reduce (fn [result item]
              (if (contains? s2 item)
                (disj result item)
                result))
            s1 s1)
    (reduce disj s1 s2)))

(defn intersection
  "Return a set that is the intersection of the input sets"
  [s1 s2]
  (if (< (count s2) (count s1))
    (intersection s2 s1)
    (reduce (fn [result item]
              (if (contains? s2 item)
                result
                (disj result item)))
            s1 s1)))

;; any input must be one of: atom set map sequence

(defn- atom-diff
  "Internal helper for diff."
  [a b]
  (if (= a b) [nil nil a] [a b nil]))

(defn equality-partition [x]
  (cond (set? x) :set
        (map? x) :map
        (vector? x) :sequence
        :else :atom))

(defn map-diff [a b]
  (let [ab-keys (union (keys a) (keys b))]
    (diff-associative a b ab-keys)))

(defn set-diff [a b]
  [(not-empty (difference a b))
   (not-empty (difference b a))
   (not-empty (intersection a b))])

(defn- vectorize
  "Convert an associative-by-numeric-index collection into
   an equivalent vector, with nil for any missing keys"
  [m]
  (when (seq m)
    (reduce
      (fn [result [k v]] (assoc result k v))
      (vec (repeat (reduce max (keys m)) nil))
      m)))

(defn sequence-diff [a b]
  (vec (map vectorize (diff-associative
                        (if (vector? a) a (vec a))
                        (if (vector? b) b (vec b))
                        (range (max (count a) (count b)))))))

(defn diff-similar [a b]
  (let [partition-a (equality-partition a)
        partition-b (equality-partition b)]
    (if (= partition-a partition-b)
      (cond
        (= partition-a :set) (set-diff a b)
        (= partition-a :map) (map-diff a b)
        (= partition-a :sequence) (sequence-diff a b)
        (= partition-a :atom) (atom-diff a b))
      (atom-diff a b))))

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
  [^{:t :any} a, ^{:t :any} b]
  (if (= a b)
    [nil nil a]
    (diff-similar a b)))
