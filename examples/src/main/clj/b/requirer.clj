(ns b.requirer
  (:require [b.required :as r]))

(defn -main ^{:t :void} [& args]
  (println (r/f 1)))
