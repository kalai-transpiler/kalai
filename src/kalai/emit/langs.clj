(ns kalai.emit.langs)

;; all available target language values
;; language values are namespaced keywords, where namespace = this namespace

(def TARGET-LANGS {::java "Java (5 and higher)"
                   ::cpp "C++ (11 and higher)"
                   ::rust "Rust"
                   ::curlybrace "category that includes ::java and :cpp"})

;; keys are the string version of the target lang keyword
;; vals are the target lang namespaced keyword
;; only keep the target langs that a user would select, not
;; any of the super-types (ex: curlybrace) useful for
;; multimethod dynamic dispatch's code sharing
(def USER-TARGET-LANG-NAMES
  (into {}
        (for [[k _] (dissoc TARGET-LANGS ::curlybrace)]
          [(name k) k])))

;; create the Clojure-style global type hierarchy for the target languages
;; (for external users who want to extend/change, there is make-hiearchy)

(derive ::java ::curlybrace)
(derive ::cpp ::curlybrace)
(derive ::rust ::curlybrace)


