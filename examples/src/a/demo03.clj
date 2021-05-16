(ns a.demo03)

(defn -main ^{:t :void} [& _args]
  (println (System/getenv "USER")))
