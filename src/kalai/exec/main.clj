(ns kalai.exec.main
  (:require [clojure.tools.cli :as cli]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [kalai.emit.langs :as l]
            [kalai.exec.kalai-to-language :as kalai-to-language]
            [kalai.exec.language-compilers :as lc]))

(def cli-options
  [["-s" "--src-dir DIRECTORY" "Input directory"
    :missing "Input directory is missing"
    :validate [(fn check-filename [filename]
                 (.exists (io/file filename)))
               "File must exist"]
    :default "src/main/clj"]
   ["-t" "--transpile-dir DIRECTORY" "Output directory"
    :default "."]
   ["-l" "--languages LANGUAGE" (str "Target language (" (str/join ", " (keys l/USER-TARGET-LANG-NAMES)) ")")
    ;;:parse-fn l/USER-TARGET-LANG-NAMES
    :default #{::l/java ::l/rust}
    :validate [some?]]
   ["-v" "--verbose" "Reports progress"]
   ["-h" "--help" "Print usage information"]])

(defn -main [& args]
  (let [{:keys [errors summary options]} (cli/parse-opts args cli-options :strict true)]
    (cond
      errors (do (println (str/join \newline errors))
                 (System/exit -1))
      (:help options) (do (println "Options:")
                          (println summary))
      :else
      (let [{:keys [verbose languages transpile-dir]} options]
        (when verbose
          (println "Options" (pr-str options)))
        (kalai-to-language/transpile-all options)
        (lc/build languages transpile-dir)
        (when verbose
          (println "Done")))))
  (shutdown-agents))
