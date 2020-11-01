(ns b.type-alias)
(def ^{:kalias {:map [:long :string]}} T)
(def ^{:t T} x)
(defn f ^{:t T} [^{:t T} y]
  (let [^{:t T} z y]
    ^:mut {}))
