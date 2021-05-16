(ns b.requirer
  (:require [b.required :as r]))

(defn -main ^{:t :void} [& _args]
  (println (r/f 1)))
