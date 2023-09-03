(ns kalai.pass.java.util
  (:require [clojure.string :as str]
            [camel-snake-kebab.core :as csk]
            [kalai.util :as u]))

(defn fully-qualified-function-identifier-str [function-name class-function-separator]
  (if (string? function-name)
    function-name
    (let [varmeta (some-> function-name meta :var meta)]
      (if (and (str/includes? (str function-name) "/") varmeta)
        (let [s (str (:ns varmeta))
              xs (str/split s #"\.")
              packagename (str/join "." (for [z (butlast xs)]
                                          (str/lower-case (csk/->camelCase z))))
              classname (csk/->PascalCase (last xs))
              full-classname (str packagename "." classname)
              function-name (csk/->camelCase (str (:name varmeta)))]
          (str full-classname class-function-separator function-name))
        (if (str/includes? function-name "-")
          (csk/->camelCase function-name)
          function-name)))))
