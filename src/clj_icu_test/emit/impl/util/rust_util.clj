(ns clj-icu-test.emit.impl.util.rust-util
  (:require [clj-icu-test.emit.interface :as iface :refer :all]
            [clojure.tools.analyzer.jvm :as az]))

(defn is-assignment-expr-atom
  "Input is assignment expression AST, not full assignment AST. Returns whether expression AST is defining an atom"
  [ast]
  (boolean
   (and (= :invoke (-> ast :op))
        (= "atom" (-> ast :fn :meta :name name)))))

(defn is-primitive-type?
  "(Adapted from curlybrace defmethod for iface/is-complex-type?)
  Return whether the type class represents a Rust primitive, given the AST directly containing the type class info"
  [ast-opts]
  (let [ast (:ast ast-opts)
        user-type (or (-> ast-opts :impl-state :type-class-ast :mtype)
                      (:mtype ast))
        is-type-user-defined (and (not (nil? user-type))
                                  (seqable? user-type)
                                  (< 1 (count user-type)))
        ast-type (or (:type ast)
                     (:op ast))
        non-scalar-types #{:seq :vector :map :set :record}
        is-non-scalar-type (get non-scalar-types ast-type)
        is-string-type (= :string ast-type)
        is-complex-type (or is-non-scalar-type
                            is-type-user-defined)
        is-primitive-type (and (not is-complex-type)
                               (not is-string-type))]
    (boolean is-primitive-type)))

(defn pass-arg-as-reference?
  "Return whether the arg of the input AST should be passed as a reference of itself or not, assuming that we already know that is an arg to a function call that is expecting it to be a reference argument."
  [ast-opts]
  (let [ast (:ast ast-opts)
        result (boolean
                (and (not (is-primitive-type? ast-opts))
                     (or (:literal? ast)
                         (not= :arg (:local ast)))))]
    result))

(defn pass-arg-as-value?
  "Similar to pass-arg-as-reference?, but indicate whether the arg should be passed as a value of itself (dereferenced refs, or .to_string() on String references)."
  [ast-opts]
  (let [ast (:ast ast-opts)
        result (boolean
                (and (not (is-primitive-type? ast-opts))
                     (not (:literal? ast))
                     (= :arg (:local ast))))]))

(def VALID-RUST-ARG-REF-STYLES #{:auto-ref :auto-val})

(defn- emit-arg-impl
  "Implementation for emit-arg for Rust.  Refactored from rust.clj.
  symb is the symbol in the input source code that is an arg to a fn call whose AST is represented in ast-opts.
  ref-style is a keyword from the set VALID-RUST-ARG-REF-STYLES indicating whether the fn call arg should be emitted as a Rust value or Rust reference, and whether or not to assume/infer that distinction when emitted the arg."
  [ast-opts symb ref-style]
  {:pre [(contains? VALID-RUST-ARG-REF-STYLES ref-style)
         (or (:env ast-opts)
             (= symb (eval symb)))]}
  (let [ast-opts-env (:env ast-opts)
        symb-class (class symb)
        symb-ast (if (seq ast-opts-env)
                   (az/analyze symb ast-opts-env)
                   (az/analyze symb))
        symb-ast-opts (assoc ast-opts :ast symb-ast)]
    (letfn [(rustified-arg-prefix [arg-ast-opts]
              (let [pass-as-reference (pass-arg-as-reference? arg-ast-opts)
                    rust-arg-prefix (when (and (= :auto-ref ref-style)
                                              pass-as-reference)
                                      "&")]
                rust-arg-prefix))
            (rustified-arg-emit [arg-ast-opts]
              (let [default-arg-str (emit arg-ast-opts)
                    rust-arg-prefix (rustified-arg-prefix arg-ast-opts)
                    arg-str-parts [rust-arg-prefix
                                   default-arg-str]
                    arg-str (apply str arg-str-parts)]
                arg-str))]
      (cond

        ;; emit a "standalone" token
        (= clojure.lang.Symbol symb-class)
        (rustified-arg-emit symb-ast-opts)
        
        ;; A seq of symbols -> emit parenthases around the emitted form. Do this for
        ;; an expression like (+ 2 3) -> (2 + 3), but don't do this for a vector
        ;; like [2 3 5] -> (Arrays.asList(2,3,5)).
        (or (isa? symb-class clojure.lang.IPersistentCollection)
            (isa? symb-class clojure.lang.ISeq))
        (cond
          (= 1 (count symb))
          (rustified-arg-emit (assoc symb-ast-opts :ast (first symb-ast)))

          (is-complex-type? ast-opts)
          (rustified-arg-emit symb-ast-opts)

          :else
          (str (rustified-arg-prefix symb-ast-opts) "(" (emit symb-ast-opts) ")"))

        ;; else, we have something that we treat like a scalar
        :else
        (rustified-arg-emit symb-ast-opts)))))

(defn emit-arg-ref
  [ast-opts symb]
  (emit-arg-impl ast-opts symb :auto-ref))

(defn emit-arg-val
  [ast-opts symb]
  ;; emit-arg-impl currently doesn't implement any automatic inferences of references and subsequent dereferencing of those references
  (emit-arg-impl ast-opts symb :auto-val))

(defn emit-args-impl
  "Similar to curlybrace emit-args but takes an extra option (same as rust emit-arg) that indicates whether caller wants this arg as pass-by-value or pass-by-reference.  Implementation copied from curlybrace defmethod for emit-args"
  [ast-opts ref-style]
  {:pre [(-> ast-opts :ast :raw-forms seq)]}
  (let [ast (:ast ast-opts)
        raw-forms (-> ast :raw-forms)
        raw-form-arg-symbols (-> raw-forms
                                 last
                                 rest)
        raw-form-arg-symbol-ast-opts (assoc ast-opts :env (-> ast :env))
        emitted-args (map #(emit-arg-impl raw-form-arg-symbol-ast-opts % ref-style) raw-form-arg-symbols)]
    emitted-args))

(defn emit-args-ref
  [ast-opts]
  (emit-args-impl ast-opts :auto-ref))

(defn emit-args-val
  [ast-opts]
  (emit-args-impl ast-opts :auto-val))
