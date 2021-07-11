(ns kalai.pass.java.util
  (:require [clojure.string :as str]
            [camel-snake-kebab.core :as csk]))

(defn fully-qualified-function-identifier-str [function-name class-function-separator]
  (if-let [metameta (some-> function-name meta :var meta)]
    (let [s (str (:ns metameta))
          xs (str/split s #"\.")
          packagename (str/join "." (for [z (butlast xs)]
                                      (str/lower-case (csk/->camelCase z))))
          classname (csk/->PascalCase (last xs))
          full-classname (str packagename "." classname)
          function-name (csk/->camelCase (str (:name metameta)))]
      (str full-classname class-function-separator function-name))
    (if (str/includes? function-name "-")
      (csk/->camelCase function-name)
      function-name)))
