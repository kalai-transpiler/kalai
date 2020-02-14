(ns clj-icu-test.emit.interface)

;;
;; dispatch fn(s)
;;

(defn lang
  [ast-opts & other-args]
  (:lang ast-opts))

(defn const-complex-type-dispatch
  "Dispatch fn for emit-const-complex-type.  Returns the complex (coll) type of the input in addition to the emitter's target lang. Return val is something like :vector, :map, :set, :record as provided by analyzer."
  [ast-opts]
  (let [target-lang (lang ast-opts)
        const-complex-type-val (or (-> ast-opts :ast :type)
                                   (-> ast-opts :ast :op))
        dispatch-val [target-lang const-complex-type-val]]
    dispatch-val))

(defn const-scalar-type-dispatch
  "Dispatch fn for emit-const-scalar-type.  In the case of curlybrace langs, currently, the emitter gets called from emit-const only when is-complex-type? returns a falsey value.  The scalar type provided in the 2nd position of the dispatch value is the keyword value for the :type key of the AST"
  [ast-opts]
  (let [target-lang (lang ast-opts)
        scalar-type (-> ast-opts
                        :ast
                        :type)
        dispatch-val [target-lang scalar-type]]
    dispatch-val))

(defn assignment-complex-type-dispatch
  "Dispatch fn for emit-assignment-complex-type.  Returns the complex (coll) type of the input in addition to the emitter's target lang. Return val is something like :vector, :map, :set, :record as provided by analyzer.
  Since in a statically typed target lang, we sometimes need the type signature of the identifier on the LHS when emitting the constructor code on the RHS, we must obtain the user-provided type accordingly."
  [ast-opts]
  (let [;;expr-ast (update-in ast-opts [:ast] :init)
        complex-expr-opts (let [nested-expr-sub-expr-opts ast-opts
                                assignment-init-expr-opts (update-in ast-opts [:ast] :init)]
                            (cond
                              (-> ast-opts :ast :init) assignment-init-expr-opts
                              :else nested-expr-sub-expr-opts))]
    (const-complex-type-dispatch complex-expr-opts ;;expr-ast
     )))

(defn complex-type-dispatch
  "Dispatch fn for emit-complex-type.  Returns the value in the AST of the user-defined type that represents the complex (coll) type."
  [ast-opts]
  (let [ast (:ast ast-opts)
        user-defined-type-ast (or (-> ast-opts :impl-state :type-class-ast :mtype)
                                  (-> ast :mtype))
        complex-type-val (first user-defined-type-ast)
        lang-val (lang ast-opts)
        dispatch-val [lang-val complex-type-val]]
    dispatch-val))

;;
;; multimethod specs
;;

(defmulti
  ^{:doc "Return whether AST represents a non-scalar / collection type"}
  is-complex-type? lang)

(defmulti
  ^{:doc "Emit the type signature for a complex (collection) type based on an AST representing the type.  The AST contains the :mtype key which holds the sub-AST that represents the type information.
Might return nil"}
  emit-complex-type complex-type-dispatch)

(defmulti
  ^{:doc "Emit the type signature for a scalar type based on an AST representing the type.
Might return nil"}
  emit-scalar-type lang)

(defmulti
  ^{:doc   "Allow the value of :mtype to be a class representing a scalar type.  But the more
consistent way would be in a vector, which can be used for a scalar type as well as
for a complex (collection) type.
Might return nil"}
  emit-type lang)

(defmulti is-number-type? lang)

(defmulti ^{:doc "Emit a statement. Input is either a string representing the statement, or a seq representing the parts of the statement (assumed that parts of space-separated)."}
  emit-statement lang)

(defmulti emit-statements lang)

(defmulti emit-block-statement-content lang)

(defmulti
  ^{:doc "indicate whether input is a string representing a statement"}
  can-become-statement lang)

(defmulti ^{:doc "emit a literal value that represents a complex (collection) type"}
  emit-const-complex-type const-complex-type-dispatch)

(defmulti ^{:doc "emit a literal value that represents a \"scalar\" (non-collection) type"}
  emit-const-scalar-type const-scalar-type-dispatch)

(defmulti ^{:doc "return a set of keywords for the :type field in the AST of a const (literal) input for which those types need to have their emitted forms customized for the target language"}
  get-custom-emitter-scalar-types lang)

(defmulti ^{:doc "emit a literal value"}
  emit-const lang)

(defmulti emit-do lang)

;; if

(defmulti emit-if lang)

(defmulti emit-cond lang)

;; bindings

(defmulti emit-atom lang)

(defmulti emit-reset! lang)

(defmulti emit-assignment-complex-type assignment-complex-type-dispatch)

(defmulti get-assignment-identifier-symbol lang)

(defmulti get-assignment-type-class-ast lang)

(defmulti emit-assignment-scalar-type lang)

(defmulti
  ^{:doc "To be used by both 'def' and any bindings block of a form (ex: let)
  Might return nil"}
  emit-assignment lang)

(defmulti ^{:doc "emit a top-level assignment"}
  emit-def lang)

(defmulti
  ^{:doc "Might return nil"}
  emit-binding lang)

(defmulti
  ^{:doc "The bindings stanza in a form (ex: in a let form).
  Might return nil"}
  emit-bindings-stanza lang)

(defmulti ^{:doc "emit a local block"}
  emit-let lang)

;; "arithmetic" (built-in operators)

(defmulti ^{:doc "Emit one of the arguments for emit-args"}
  emit-arg lang)

(defmulti ^{:doc "Emit the arguments of a static call"}
  emit-args lang)

(defmulti ^{:doc "Emit a \"static call\" fn (happens to be implemented in Clojure as a static call.  Includes syntactic operators, get, etc."}
  emit-static-call lang)

(defmulti ^{:doc "Emit an arithmetic operation with a built-in operator/fn"}
  emit-syntactic-operator lang)

;; other built-in fns (also marked with op = :static-call)

(defmulti ^{:doc "Emit a get call to a map"}
  emit-get lang)

(defmulti ^{:doc "Emit an nth call to a vector/sequence"}
  emit-nth lang)

;; other

(defmulti ^{:doc "Emit a local binding var (stored in :local key in AST by analyzer)"}
  emit-local lang)

(defmulti ^{:doc "Emit a var (stored in :var key in AST by analyzer)"}
  emit-var lang)

;; defn

(defmulti ^{:doc "Emit one of the arguments in emit-defn-args"}
  emit-defn-arg lang)

(defmulti ^{:doc "Emit the args for defn, which require a type signature in static languages"}
  emit-defn-args lang)

(defmulti
  ^{:doc "currently does not handle variadic fns (fn overloading)"}
  emit-defn lang)

;; emitter common impl forms (classes, enums, etc.)

(defmulti emit-defclass lang)

(defmulti emit-defenum lang)

(defmulti emit-return lang)

;; deref

(defmulti emit-deref lang)

;; not

(defmulti emit-not lang)

(defmulti emit-not= lang)

;; contains?

(defmulti emit-contains? lang)

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
  ^{:doc "Insert a character into a string buffer at a particular index."}
  emit-insert-strbuf-char lang)

(defmulti
  ^{:doc "Insert a string into a string buffer at a particular index."}
  emit-insert-strbuf-string lang)

(defmulti
  ^{:doc "Emit the production of a string from a string buffer in C++"}
  emit-tostring-strbuf lang)

(defmulti
  ^{:doc "Emit the expression that gives the length of the given string buffer"}
  emit-length-strbuf lang)

(defmulti
  ^{:doc "Emit the expression that gives the length of the given string expression"}
  emit-strlen lang)

(defmulti emit-str-eq lang)

(defmulti
  ^{:doc "Emit the expression that gives the character at the given index of the given string expression"}
  emit-str-char-at lang)

(defmulti emit-seq-length lang)

(defmulti emit-seq-append lang)

(defmulti
  ^{:doc "handles invocations of known functions"}
  emit-invoke lang)

;; loops (ex: while, doseq)

(defmulti emit-while lang)

(defmulti emit-loop lang)

(defmulti
  ^{:doc "Emit a dotimes form (aka \"for loop\" with a 0..N-1 iteration)"}
  emit-dotimes lang)

;; ns

(defmulti emit-ns lang)

;; special forms

(defmulti emit-new lang)

;; metadata

(defmulti
  ^{:doc "For now, assume that :with-meta can be ignored, and that if the information
is important, it will have been gotten elsewhere (ex: emit-assignment* fns)."}
  emit-with-meta lang)

;; entry point

(defmulti emit lang)
