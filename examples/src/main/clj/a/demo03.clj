(ns a.demo03)

(defn -main ^{:t :void} [& args]
  (println (System/getenv "USER")))
