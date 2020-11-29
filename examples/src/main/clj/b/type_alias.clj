(ns b.type-alias)
(def ^{:kalias {:mmap [:long :string]}} T)
(def ^{:t T} x nil)
(defn f ^{:t T} [^{:t T} y]
  (let [^{:t T} z y]
    z))
