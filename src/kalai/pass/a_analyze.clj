(ns kalai.pass.a-analyze
  (:require [clojure.tools.analyzer.jvm :as az]
            [clojure.tools.analyzer.passes.jvm.emit-form :as e]))

;; We chose to match sexpressions rather than ast maps because we are more familiar with sexpressions.
;; Matching maps might not be bad, it might be better.
;; Matching maps is certainly more powerful as the sexpressions elide information in the ast.
;; We don't seem to need the full ast (yet) because we are only concerned with transformation, not meaning.

(defn analyze
  "Given a file-path containing a namespace, returns forms found in the namespace.
  The forms are not the original forms, they are emitted by tools analyzer and so have a more standard structure."
  [file-path]
  (->> (az/analyze-ns file-path)
       (map e/emit-form)))
