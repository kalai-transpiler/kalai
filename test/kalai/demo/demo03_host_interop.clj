(ns kalai.demo.demo03-host-interop
  (:require [kalai.common :refer :all]))

(defclass "HostInterop"
          (defn printEnvVariables ^String
            []
            (let [^String envEditor (System/getenv "EDITOR")
                  testEnd (.endsWith envEditor "X")]
              (return envEditor))))