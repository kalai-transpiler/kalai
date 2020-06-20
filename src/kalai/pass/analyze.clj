(ns kalai.pass.analyze
  (:require [clojure.tools.analyzer.jvm :as az]
            [clojure.tools.analyzer.passes.jvm.emit-form :as e]))

(defn analyze
  "Given a file-path containing a namespace, returns forms found in the namespace.
  The forms are not the original forms, they are emitted by tools analyzer and so have a more standard structure."
  [file-path]
  (->> (az/analyze-ns file-path)
       (map e/emit-form)))
