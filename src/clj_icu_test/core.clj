(ns clj-icu-test.core
  (:require [clojure.edn :as edn]
            [clojure.string :as string]
            [clojure.tools.analyzer.jvm :as az]))

;;
;; constants
;;

(def INDENT-CHAR \space)

(def INDENT-LEVEL-INCREMENT 2)

(def LANGS {:java :cpp})

;;
;; indentation - global state and fns
;;

(def indent-level (atom 0))

(defn reset-indent-level [] (reset! indent-level 0))

(defmacro indent
  [& body] 
  `(do
     (swap! indent-level + INDENT-LEVEL-INCREMENT)
     (let [result# (do ~@body)]
       ;; force lazy sequences to evaluate now so that they use
       ;; the current state of the indent level
       (when (seq result#)
         (doall result#))
       (swap! indent-level - INDENT-LEVEL-INCREMENT) 
       result#)))

(defn indent-str-curr-level
  []
  (apply str (repeat @indent-level INDENT-CHAR)))

;;
;; records
;;

(defrecord AstOpts [ast env lang])

(defn new-ast-opts
  "keys of the opts map are the fields in AstOpts record"
  [opts]
  {:pre [(or (not (:lang opts))
             (get LANGS (:lang opts)))]}
  (map->AstOpts opts))

;;
;; impl fns and macros
;;

(defn defclass
  "Create a cosmetic fn that allows us to organize the forms in body"
  [name & body]
  ;; Note: everything in body will get evaluated as a result of being arguments passed
  ;; to the function.  The fn creates a name to be used when searching the AST to detect
  ;; this semantic.
  ;; Note: this trick of defining semantics in the AST through custom fns should be able
  ;; to work even in a nested fashion, AFAICT
  ;; Note: the return value shouldn't matter since the code/S-expressions in Clojure may
  ;; not result in working Clojure code, let alone the best/most efficient code.
  ;; Note: originally tried using a pass-through macro or wrapping the body inside a do
  ;; form, but then the original macro nor the do block show up in the AST in an easily
  ;; recognizable way.  In the future, if a macro is truly needed, the combination of
  ;; a macro and a function give the full power of expression (not requiring name to
  ;; be of a certain type) while still being easy to recognize in the AST.
  nil)

(defn defenum-impl
  "This is the fn in the macro + fn trick described in defclass above to allow
  ease of use for users while still preserving retrievability of info in AST"
  [name & body]
  ;; Note: similar notes as those for defclass
  nil)

(defmacro defenum
  "This is the macro in the macro + fn trick.  See defenum-impl and defclass."
  [name & body]
  (let [quoted-field-names (map str body)]
    (list* 'defenum-impl name quoted-field-names)))

(defn return
  "A pass-through function that defines \"return\" as a form"
  [expr]
  expr)

;; impl fns - string-related

(defn new-strbuf
  "This fn represents creating a string buffer in the target language.
  For the purposes of the JVM-based analyzer, return a StringBuffer"
  []
  (new StringBuffer))

(defn prepend-strbuf
  "This fn represents prepending a string into an accumulator string buffer in the target language."
  [strbuf s]
  (.insert strbuf 0 s))

(defn tostring-strbuf
  "This fn represents producing the final string value of a string buffer"
  [strbuf]
  (.toString strbuf))

;;
;; AST helper fns
;;

(defn fn-matches?
  "indicates whether the function AST's meta submap matches the fn with the given fully-qualified namespace string and name.
  Takes an AST and returns a boolean."
  [fn-meta-ast exp-ns-str exp-name-str]
  (let [fn-ns (:ns fn-meta-ast)
        fn-name (-> fn-meta-ast :name)
        exp-ns (find-ns (symbol exp-ns-str))]
    (boolean (and (= fn-ns exp-ns)
                  (= (name fn-name) exp-name-str)))))

(defn instance-call-matches?
  "Given a map with keys :inst-call-ast (AST), :exp-method-name (type=String), and :exp-instance-class (type=Class),
  return whether the instance call's AST matches the expected instance obj type and method.
  Returns a boolean"
  [{:keys [inst-call-ast exp-method-name exp-instance-class]}]
  (let [inst-class (-> inst-call-ast
                       :instance
                       :tag)
        method-name (-> inst-call-ast
                        :method
                        str)]
    (boolean (and (= inst-class exp-instance-class)
                  (= method-name exp-method-name)))))
