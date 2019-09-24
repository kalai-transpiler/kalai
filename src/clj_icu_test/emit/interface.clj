(ns clj-icu-test.emit.interface)

;;
;; dispatch fn(s)
;;

(defn lang
  [ast-opts & other-args]
  (:lang ast-opts))

;; to be used on the AST of a data literal of
;; a complex (collection/aggregate) type
(defn const-complex-type-dispatch
  [ast-opts]
  (letfn [(const-complex-type-fn
            [ast-opts]
            (or (-> ast-opts :ast :type)
                (-> ast-opts :ast :op)))
          (get-dispatch-fn
            [ast-opts]
            (juxt lang const-complex-type-fn))]
    (let [dispatch-fn (get-dispatch-fn ast-opts)
          dispatch-val (dispatch-fn ast-opts)]
      dispatch-val)))

;; for the specific value of the complex type, use
;; the expression on RHS to get the specific type val,
;; not the user-provided type in the metadata of the
;; identifier name.
(defn assignment-complex-type-dispatch
  [ast-opts]
  (let [expr-ast (update-in ast-opts [:ast] :init)]
    (const-complex-type-dispatch expr-ast)))

;;
;; multimethod specs
;;

(defmulti
  ^{:doc "Return whether AST represents a non-scalar / collection type"}
  is-complex-type? lang)

(defmulti
  ^{:doc "Might return nil"}
  emit-complex-type lang)

(defmulti
  ^{:doc "Might return nil"}
  emit-scalar-type lang)

(defmulti
  ^{:doc "Might return nil"}
  emit-type lang)

(defmulti is-number-type? lang)

(defmulti emit-statement lang)

(defmulti
  ^{:doc "indicate whether input is a string representing a statement"}
  can-become-statement lang)

(defmulti emit-const-complex-type const-complex-type-dispatch)

(defmulti emit-const lang)

(defmulti emit-do lang)

;; bindings

(defmulti emit-atom lang)

(defmulti emit-reset! lang)

(defmulti emit-assignment-complex-type assignment-complex-type-dispatch)

(defmulti get-assignment-identifier-symbol lang)

(defmulti get-assignment-type-class-ast lang)

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
