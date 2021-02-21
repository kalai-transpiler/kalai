(ns kalai.exec.main
  (:require [clojure.tools.cli :as cli]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [kalai.emit.langs :as l]
            [kalai.exec.kalai-to-language :as kalai-to-language]))

(def cli-options
  [["-i" "--in DIRECTORY" "Input directory"
    :missing "Input directory is missing"
    :validate [(fn check-filename [filename]
                 (.exists (io/file filename)))
               "File must exist"]]
   ["-o" "--out DIRECTORY" "Output directory"
    :default "out"]
   ["-l" "--language LANGUAGE" (str "Target language (" (str/join ", " (keys l/USER-TARGET-LANG-NAMES)) ")")
    :parse-fn l/USER-TARGET-LANG-NAMES
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
      (let [{:keys [verbose]} options]
        (when verbose
          (println "Options" (pr-str options)))
        (kalai-to-language/transpile-all options)
        (when verbose
          (println "Done"))))))
