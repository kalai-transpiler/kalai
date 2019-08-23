(ns clj-icu-test.emit.interface)

;;
;; dispatch fn(s)
;;

(defn lang
  [ast-opts & other-args]
  (:lang ast-opts))

;;
;; multimethod specs
;;



(defmulti emit-const lang)

(defmulti emit-do lang)

;; bindings

(defmulti emit-atom lang)

(defmulti emit-reset! lang)

(defmulti
  ^{:doc "To be used by both 'def' and any bindings block of a form (ex: let)
  Might return nil"}
  emit-assignment lang)

(defmulti emit-def lang)

(defmulti
  ^{:doc "Might return nil"}
  emit-binding lang)

(defmulti
  ^{:doc "The bindings stanza in a form (ex: in a let form).
  Might return nil"}
  emit-bindings-stanza lang)

(defmulti emit-let lang)

;; "arithmetic" (built-in operators)

(defmulti emit-arg lang)

(defmulti emit-args lang)

(defmulti emit-static-call lang)

;; other

(defmulti emit-local lang)

(defmulti emit-var lang)

;; defn

(defmulti emit-defn-arg lang)

(defmulti emit-defn-args lang)

(defmulti
  ^{:doc "currently does not handle variadic fns (fn overloading)"}
  emit-defn lang)

;; emitter common impl forms

(defmulti emit-defclass lang)

(defmulti emit-defenum lang)

(defmulti emit-return lang)

;; deref

(defmulti emit-deref lang)

;; not

(defmulti emit-not lang)

;; fn invocations

(defmulti emit-invoke-arg lang)

(defmulti emit-invoke-args lang)

(defmulti emit-str-arg lang)

(defmulti emit-str-args lang)

(defmulti emit-str lang) 

(defmulti
  ^{:doc "Emit the equivalent of printing out to std out. To support auto-casting to str, insert a \"\" before the other args"}
  emit-println lang)

(defmulti
  ^{:doc "Emit an instantiation of a string buffer in C++ as a string"}
  emit-new-strbuf lang)

(defmulti
  ^{:doc "Emit the prepending of a string to a string buffer in C++.
  A C++ string is mutable, so it will be used as the buffer.
  First arg in the AST is the string (mutable string = string
  buffer), second arg is the string that needs to be prepended"}
  emit-prepend-strbuf lang)

(defmulti
  ^{:doc "Emit the production of a string from a string buffer in C++"}
  emit-tostring-strbuf lang)

(defmulti
  ^{:doc "handles invocations of known functions"}
  emit-invoke lang)

;; loops (ex: while, doseq)

(defmulti emit-while lang)

(defmulti emit-loop lang)

;; special forms

(defmulti emit-new lang)

;; entry point

(defmulti emit lang)
