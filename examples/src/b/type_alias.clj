(ns b.type-alias)

(def ^{:kalias {:mmap [:long :string]}} T)
(def ^{:t T} x {})
(defn f ^{:t T} [^{:t T} y]
  (let [^{:t T} z y]
    z))

(defn -main ^{:t :void} [& _args]
  ;;(f ^{:t T} {1 "one"})
  (println "OK"))
