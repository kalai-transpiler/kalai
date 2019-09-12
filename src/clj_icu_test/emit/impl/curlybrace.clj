(ns clj-icu-test.emit.impl.curlybrace
  (:require [clj-icu-test.common :refer :all]
            [clj-icu-test.emit.interface :as iface :refer :all]
            [clj-icu-test.emit.langs :as l]
            [clojure.edn :as edn] 
            [clojure.string :as string]
            [clojure.tools.analyzer.jvm :as az]))

;;
;; this namespace is for C-style syntax emitting fns
;; (could be used for C++, Java, and/or others)
;;

(defmethod iface/is-complex-type? ::l/curlybrace
  [ast-opts]
  (let [ast (:ast ast-opts)
        ;; TODO: find if it is possible to re-use code from tools.analyzer to determine scalar vs. complex/aggregate data
        ast-type (:type ast)
        is-type-user-defined (and (not (nil? ast-type))
                                  (seqable? ast-type))
        non-scalar-types #{:seq :vector :map :set}
        is-non-scalar-type (get non-scalar-types ast-type)
        is-complex-type (or is-non-scalar-type
                            is-type-user-defined)]
    (boolean is-complex-type)))

(defmethod iface/emit-type ::l/curlybrace
  [ast-opts]
  (if (is-complex-type? ast-opts)
    (emit-complex-type ast-opts)
    (emit-scalar-type ast-opts)))

(defmethod iface/emit-const ::l/curlybrace
  [ast-opts]
  {:pre [(= :const (:op (:ast ast-opts)))
         (:literal? (:ast ast-opts))]}
  (let [ast (:ast ast-opts)]
    (pr-str (:val ast))))

(defmethod iface/emit-do ::l/curlybrace
  [ast-opts]
  {:pre [(= :do (:op (:ast ast-opts)))]}
  (let [ast (:ast ast-opts)
        stmts (:statements ast)
        stmts-ast-opts (map (partial assoc ast-opts :ast) stmts)
        stmt-emitted-lines (map emit stmts-ast-opts)
        last-stmt (:ret ast)
        last-emitted-line (emit (assoc ast-opts :ast last-stmt))
        last-emitted-line-val-opts (map->AnyValOpts (assoc ast-opts :val last-emitted-line))
        last-emitted-line-as-stmt (emit-statement last-emitted-line-val-opts)
        all-lines (concat stmt-emitted-lines [last-emitted-line-as-stmt])]
    all-lines))

(defmethod iface/emit-atom ::l/curlybrace
  [ast-opts]
  {:pre [(and (= :invoke (:op (:ast ast-opts)))
              (= (symbol "atom") (-> ast-opts :ast :fn :meta :name)))]}
  (let [ast (:ast ast-opts)
        init-val-ast (-> ast
                         :args
                         first)]
    (emit (assoc ast-opts :ast init-val-ast))))

(defmethod iface/emit-reset! ::l/curlybrace
  [ast-opts]
  {:pre [(and (= :invoke (:op (:ast ast-opts)))
              (= (symbol "reset!") (-> ast-opts :ast :fn :meta :name)))]}
  (let [ast (:ast ast-opts)
        identifier (str (or (-> ast :args first :meta :name)
                            (-> ast :args first :form)))
        reset-val-ast (-> ast
                          :args
                          second)
        expression (emit (assoc ast-opts :ast reset-val-ast))
        statement-parts [identifier
                         "="
                         expression]
        statement-parts-opts (-> ast-opts
                                 (assoc :val statement-parts)
                                 map->AnyValOpts)
        statement (emit-statement statement-parts-opts)]
    statement))

(defmethod iface/get-assignment-type-class-ast ::l/curlybrace
  [ast-opts]
  (let [ast (:ast ast-opts)
        op-code (:op ast)]
    (cond
      (get-in ast [:meta :val :tag])
      (get-in ast [:meta :val])

      (get-in ast [:init :env :tag])
      (get-in ast [:init :en])
      
      (and (= :binding op-code)
           (get ast :tag))
      ast

      (:type ast)
      ast

      (get-in ast [:meta :val :type])
      (get-in ast [:meta :val]))))

(defmethod iface/get-assignment-identifier-symbol ::l/curlybrace
  [ast-opts]
  (let [ast (:ast ast-opts)
        op-code (:op ast)
        identifier-symbol (or (get-in ast [:env :form])
                              (case op-code
                                :binding (get ast :form)
                                :def (get ast :name)))]
    identifier-symbol))

(defmethod iface/emit-assignment ::l/curlybrace
  [ast-opts]
  (let [ast (:ast ast-opts)]
    (if (= :vector (or (get-in ast [:init :type])
                       (get-in ast [:init :op])))
      (emit-assignment-vector ast-opts)
      (let [op-code (:op ast)
            type-class-ast (get-assignment-type-class-ast ast-opts)
            type-class-opts (assoc ast-opts :ast type-class-ast) 
            type-str (emit-type type-class-opts)
            identifier (when-let [identifer-symbol (get-assignment-identifier-symbol ast-opts)]
                         (str identifer-symbol))
            expression (emit (assoc ast-opts :ast (:init ast)))
            statement-parts [type-str
                             identifier
                             "="
                             expression]
            statement-parts-opts (-> ast-opts
                                     (assoc :val statement-parts)
                                     map->AnyValOpts)
            statement (emit-statement statement-parts-opts)]
        statement))))

(defmethod iface/emit-def ::l/curlybrace
  [ast-opts]
  {:pre [(= :def (:op (:ast ast-opts)))]}
  (let [ast (:ast ast-opts)]
    (emit-assignment ast-opts)))

(defmethod iface/emit-binding ::l/curlybrace
  [ast-opts]
  {:pre [(= :binding (:op (:ast ast-opts)))]}
  (let [ast (:ast ast-opts)]
    (emit-assignment ast-opts)))

(defmethod iface/emit-bindings-stanza ::l/curlybrace
  [ast-opts]
  {:pre [(sequential? (:ast ast-opts))]}
  (let [ast (:ast ast-opts)
        bindings ast ;; the AST in this case is a vector of sub-ASTs
        bindings-ast-opts (map (partial assoc ast-opts :ast) bindings)
        binding-statements (map emit-binding bindings-ast-opts)]
    (when-let [non-nil-binding-statements (->> binding-statements
                                               (keep identity)
                                               seq)]
      (let [bindings-str (string/join "\n" non-nil-binding-statements)] 
        bindings-str))))

(defmethod iface/emit-let ::l/curlybrace
  [ast-opts]
  {:pre [(= :let (:op (:ast ast-opts)))]}
  (let [ast (:ast ast-opts)
        bindings (:bindings ast)
        binding-str (indent (emit-bindings-stanza (assoc ast-opts :ast bindings)))
        body-ast (:body ast)
        body-strs (indent
                   (if (:statements body-ast)
                     ;; if ast has key nesting of [:body :statements], then we have a multi-"statement" expression do block in the let form
                     (let [butlast-statements (:statements body-ast)
                           last-statement (:ret body-ast)
                           statements (concat butlast-statements [last-statement])
                           statement-ast-opts (map #(assoc ast-opts :ast %) statements)
                           statement-strs (map emit statement-ast-opts)]
                       statement-strs)
                     ;; else the let block has only one "statement" in the do block
                     [(emit (assoc ast-opts :ast body-ast))]))
        body-strs-opts-seq (map #(-> ast-opts
                                     (assoc :val %)
                                     map->AnyValOpts)
                                body-strs)
        body-strs-with-semicolons (indent
                                   (map #(if-not (can-become-statement %)
                                           (:val %)
                                           (emit-statement %))
                                        body-strs-opts-seq))
        body-str (string/join "\n" body-strs-with-semicolons)
        block-str-parts [(str (indent-str-curr-level) "{")
                         binding-str
                         body-str
                         (str (indent-str-curr-level) "}")]        
        block-str (->> block-str-parts
                       (keep identity)
                       (string/join "\n"))] 
    block-str))


;; "arithmetic" (built-in operators)

(defmethod iface/emit-arg ::l/curlybrace
  [ast-opts symb]
  {:pre [(:env ast-opts)]}
  (let [ast-opts-env (:env ast-opts)
        symb-class (class symb)
        symb-ast (az/analyze symb ast-opts-env)
        symb-ast-opts (assoc ast-opts :ast symb-ast)]
    (cond

      ;; emit a "standalone" token
      (= clojure.lang.Symbol symb-class)
      (emit symb-ast-opts)
      
      ;; a seq of symbols -> emit parenthases around the emitted form
      (or (isa? symb-class clojure.lang.IPersistentCollection)
          (isa? symb-class clojure.lang.ISeq))
      (if (= 1 (count symb-ast))
        (emit (assoc symb-ast-opts :ast (first symb-ast)))
        (str "(" (emit symb-ast-opts) ")"))

      ;; else, we have something that we treat like a scalar
      :else (emit symb-ast-opts))))

(defmethod iface/emit-args ::l/curlybrace
  [ast-opts]
  {:pre [(-> ast-opts :ast :raw-forms seq)]}
  (let [ast (:ast ast-opts)
        raw-forms (-> ast :raw-forms)
        raw-form-arg-symbols (-> raw-forms
                                 last
                                 rest)
        raw-form-arg-symbol-ast-opts (assoc ast-opts :env (-> ast :env))
        emitted-args (map (partial emit-arg raw-form-arg-symbol-ast-opts) raw-form-arg-symbols)]
    emitted-args))

(defmethod iface/emit-static-call ::l/curlybrace
  [ast-opts]
  {:pre [(= :static-call (:op (:ast ast-opts)))]}
  (let [ast (:ast ast-opts)
        static-call-fn-symbol (let [fn-symbol (-> ast :raw-forms last first)
                                    fn-str (str fn-symbol)]
                                (case  fn-str
                                  "quot" "/"
                                  "rem" "%"
                                  ;; Note: extra work is required if supporting Clojure expressions using =
                                  ;; with more than 2 expression arguments.  Not really high
                                  ;; priority at the moment to support > 2 args for =
                                  "=" "=="
                                  fn-str))
        arg-strs (emit-args ast-opts)
        expression-parts (interpose static-call-fn-symbol arg-strs)
        expression (string/join " " expression-parts)]
    expression))

(defmethod iface/emit-local ::l/curlybrace
  [ast-opts]
  {:pre [(= :local (:op (:ast ast-opts)))]}
  (let [ast (:ast ast-opts)
        form (:form ast)]
    (str (name form))))

(defmethod iface/emit-var ::l/curlybrace
  [ast-opts]
  {:pre [(= :var (:op (:ast ast-opts)))]}
  (let [ast (:ast ast-opts)
        form (:form ast)]
    (str (name form))))

;; functions

(defmethod iface/emit-defn-arg ::l/curlybrace
  [ast-opts]
  {:pre [(= :binding (-> ast-opts :ast :op))]}
  (let [ast (:ast ast-opts)
        arg-name (-> ast :form name) 
        type-class-opts ast-opts 
        type-str (emit-type type-class-opts)
        identifier-signature-parts [type-str
                                    arg-name]
        identifier-signature (->> identifier-signature-parts
                                  (keep identity)
                                  (string/join " "))]
    identifier-signature))

(defmethod iface/emit-defn-args ::l/curlybrace
  [ast-opts]
  ;; Note: can have empty args
  ;;{:pre [(seq (:ast ast-opts))]}
  (let [ast (:ast ast-opts)
        arg-ast-seq ast
        arg-ast-opts (->> arg-ast-seq
                          (map #(assoc ast-opts :ast %))
                          (map emit-defn-arg))]
    (string/join ", " arg-ast-opts)))


;; return statement

(defmethod iface/emit-return ::l/curlybrace
  [ast-opts]
  {:pre [(= :invoke (-> ast-opts :ast :op))]}
  (let [ast (:ast ast-opts)
        expr-ast (-> ast :args first)
        expr-ast-opts (assoc ast-opts :ast expr-ast)
        expr-ast-str (emit expr-ast-opts)
        expr-ast-str-opts (-> ast-opts
                              (assoc :val ["return"
                                           expr-ast-str])
                              map->AnyValOpts)
        return-stmt-str (emit-statement expr-ast-str-opts)]
    return-stmt-str))

;; deref

(defmethod iface/emit-deref ::l/curlybrace
  [ast-opts]
  {:pre [(= :invoke (-> ast-opts :ast :op))]}
  (let [ast (:ast ast-opts)
        ;; Note: assuming that there is only one arg to deref, which is the symbol (identifier)
        identifier-symbol (-> ast :args first :form)
        identifier-str (str identifier-symbol)]
    identifier-str))

;; not

(defmethod iface/emit-not ::l/curlybrace
  [ast-opts]
  {:pre [(= :invoke (-> ast-opts :ast :op))]}
  (let [ast (:ast ast-opts)
        ;; Note: assuming that not only has 1 arg
        arg-ast (-> ast :args first)
        arg-str (emit (assoc ast-opts :ast arg-ast))
        expr-str (str "!(" arg-str ")")]
    expr-str))

;; fn invocations

(defmethod iface/emit-invoke-arg ::l/curlybrace
  [ast-opts]
  (emit ast-opts))

(defmethod iface/emit-invoke-args ::l/curlybrace
  [ast-opts]
  (let [ast (:ast ast-opts)
        args-ast (:args ast)
        args-ast-opts (map #(assoc ast-opts :ast %) args-ast)
        emitted-args (map emit-invoke-arg args-ast-opts)]
    emitted-args))

;; loops (ex: while, doseq)

(defmethod iface/emit-while ::l/curlybrace
  [ast-opts]
  {:pre [(= :loop (:op (:ast ast-opts)))]}
  (let [ast (:ast ast-opts)
        test-ast (-> ast :body :test)
        test-str (emit (assoc ast-opts :ast test-ast))
        then-ast (-> ast :body :then)
        ;; Note: we are ignoring the "return" expression stored in (-> ast :body :then :ret)
        ;; when we only look at (-> ast :body :then :statements)
        statements (:statements then-ast)
        body-strs (indent
                   (let [statement-ast-opts (map #(assoc ast-opts :ast %) statements)
                         statement-strs (map emit statement-ast-opts)]
                     statement-strs))
        body-strs-opts-seq (map #(-> ast-opts
                                     (assoc :val %)
                                     map->AnyValOpts)
                                body-strs)
        body-strs-with-semicolons (indent
                                   (map #(if-not (can-become-statement %)
                                           (:val %)
                                           (emit-statement %))
                                        body-strs-opts-seq))
        body-str (string/join "\n" body-strs-with-semicolons)
        while-parts [(str (indent-str-curr-level) "while (" test-str ")")
                     (str (indent-str-curr-level) "{")
                     body-str
                     (str (indent-str-curr-level) "}")]
        while-str (string/join "\n" while-parts)]
    while-str))

(defmethod iface/emit-loop ::l/curlybrace
  [ast-opts]
  {:pre [(= :loop (:op (:ast ast-opts)))]}
  (let [ast (:ast ast-opts)
        form-symbol (-> ast :raw-forms first first)
        form-symbol-str (str form-symbol)]
    (case form-symbol-str
      "while" (emit-while ast-opts))))

;; entry point

(defmethod iface/emit ::l/curlybrace
  [ast-opts]
  (let [ast (:ast ast-opts)]
    ;; TODO: some multimethod ?
    (case (:op ast)
      :def (case (some-> ast :raw-forms last first name)
             "defn" (emit-defn ast-opts)
             (emit-def ast-opts))
      :const (emit-const ast-opts)
      :invoke (case (-> ast :fn :meta :name name)
                "atom" (emit-atom ast-opts)
                "reset!" (emit-reset! ast-opts)
                (emit-invoke ast-opts)) 
      :do (emit-do ast-opts)
      :let (emit-let ast-opts)
      :local (emit-local ast-opts)
      :static-call (emit-static-call ast-opts)
      :var (emit-var ast-opts)
      :loop (emit-loop ast-opts)
      :new (emit-new ast-opts)
      :else (cond 
              (:raw-forms ast)
              (emit (assoc ast-opts :ast (-> ast :raw-forms)))))))
