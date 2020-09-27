(ns kalai.demo.demo03-host-interop
  (:refer-clojure :exclude [format])
  (:require [kalai.common :refer :all]))

(defclass "HostInterop"
          (defn printEnvVariables ^String
            []
            (let [^String envEditor (System/getenv "EDITOR")]
              (return envEditor))))