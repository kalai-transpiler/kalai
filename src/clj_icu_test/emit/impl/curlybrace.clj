(ns clj-icu-test.emit.impl.curlybrace
  (:require [clj-icu-test.common :refer :all]
            [clj-icu-test.emit.impl.util.curlybrace-util :as cb-util]
            [clj-icu-test.emit.interface :as iface :refer :all]
            [clj-icu-test.emit.langs :as l]
            [clojure.edn :as edn] 
            [clojure.string :as string]
            [clojure.tools.analyzer.jvm :as az])
  (:import [java.util List Map]))

;;
;; this namespace is for C-style syntax emitting fns
;; (could be used for C++, Java, and/or others)
;;

;; requires the AST directly containing the type class info to be passed in
;; (ex: the metadata map of a def form, or the AST of the expression of a collection literal
;; in a def form).
(defmethod iface/is-complex-type? ::l/curlybrace
  [ast-opts]
  (let [ast (:ast ast-opts)
        ;; TODO: find if it is possible to re-use code from tools.analyzer to determine scalar vs. complex/aggregate data
        user-type (or

                   (-> ast-opts :impl-state :type-class-ast :mtype)
                   (:mtype ast)

                   )
        is-type-user-defined (and (not (nil? user-type))
                                  (seqable? user-type)
                                  (< 1 (count user-type))
                                  )
        ast-type (or (:type ast)
                     (:op ast))
        non-scalar-types #{:seq :vector :map :set :record}
        is-non-scalar-type (get non-scalar-types ast-type)
        is-complex-type (or is-non-scalar-type
                            is-type-user-defined)]
    (boolean is-complex-type)))

(defmethod iface/emit-type ::l/curlybrace
  [ast-opts]
  (if (is-complex-type? ast-opts)
    (let [type-val (or

                    (-> ast-opts :impl-state :type-class-ast :mtype)
                    (-> ast-opts :ast :mtype)
                    (-> ast-opts :impl-state :type-class-ast :mtype)

                    )]
      (assert (sequential? type-val))
      (if (= 1 (count type-val))
        (let [type-as-tag-ast-opts (if (-> ast-opts :impl-state :type-class-ast :mtype)
                                     (-> ast-opts
                                         (update-in [:impl-state :type-class-ast :mtype] first)
                                         (update-in [:ast :mtype] first))
                                     (update-in ast-opts [:ast :mtype] first))]
          (emit-type type-as-tag-ast-opts))
        (emit-complex-type ast-opts)))
    (let [type-val (or

                    (-> ast-opts :impl-state :type-class-ast :mtype)
                    (-> ast-opts :ast :mtype)
                    (-> ast-opts :impl-state :type-class-ast :mtype)

                    )]
      (if (and (sequential? type-val)
               (= 1 (count type-val)))
        (let [simplified-type-val (first type-val)
              simplified-type-ast-opts (-> ast-opts
                                           (assoc-in [:impl-state :type-class-ast :mtype] simplified-type-val)
                                           (assoc-in [:ast :mtype] simplified-type-val))]
          (emit-scalar-type simplified-type-ast-opts))
        (emit-scalar-type ast-opts)))))

(defmethod iface/emit-const-scalar-type [::l/curlybrace :char]
  [ast-opts]
  (let [char-val (-> ast-opts
                     :ast
                     :val)]
    (str \' char-val \')))

(defmethod iface/emit-const ::l/curlybrace
  [ast-opts]
  {:pre [(= :const (:op (:ast ast-opts)))
         (:literal? (:ast ast-opts))]}
  (if (is-complex-type? ast-opts)
    (emit-const-complex-type ast-opts)
    (let [custom-emitter-scalar-types #{:char}
          ast (:ast ast-opts)
          scalar-type (:type ast)]
      ;; Clojure's syntax literals are pretty much the same as
      ;; curlybrace langs like Java/C++ but differs in some cases like
      ;; for characters
      (if (contains? custom-emitter-scalar-types scalar-type)
        (emit-const-scalar-type ast-opts)
        (pr-str (:val ast))))))

(defmethod iface/emit-statements ::l/curlybrace
  [val-opts]
  {:pre [(= clj_icu_test.common.AnyValOpts (class val-opts))]}
  (let [statement-parts-seq (:val val-opts)
        statement-parts-opts-seq (for [statement-parts statement-parts-seq]
                                   (assoc val-opts :val statement-parts))
        statement-strs (map emit-statement statement-parts-opts-seq)
        all-statements-str (string/join "\n" statement-strs)]
    all-statements-str))

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
      (get-in ast [:init :env])
      
      (and (= :binding op-code)
           (get ast :tag))
      ast

      (:type ast)
      ast

      (get-in ast [:meta :val :mtype])
      (get-in ast [:meta :val])

      ;; when using tools.analyzer's analyze-ns, the user-supplied type (:mtype) in
      ;; metadata gets parsed differently, which is most easily recovered in the
      ;; same way by re-evaluating the form of symbols stored in :form of the AST.
      ;;
      ;; Note: *** this uses eval ***
      ;;
      ;; TODO: don't use eval
      (get-in ast [:meta :form :mtype])
      (do
        (import '[java.util List Map])
        (let [type-class-form (get-in ast [:meta :form :mtype])
              type-class-val (eval type-class-form)
              type-class-ast {:mtype type-class-val}]
          type-class-ast)))))

(defmethod iface/get-assignment-identifier-symbol ::l/curlybrace
  [ast-opts]
  (let [ast (:ast ast-opts)
        op-code (:op ast)
        identifier-symbol (or (get-in ast [:env :form])
                              (case op-code
                                :binding (get ast :form)
                                :def (get ast :name)))]
    identifier-symbol))

(defmethod iface/emit-assignment-complex-type [::l/curlybrace :invoke]
  [ast-opts]
  {:pre [(= :invoke (-> ast-opts :ast :init :op))]}
  (let [ast (:ast ast-opts)
        type-class-ast (get-assignment-type-class-ast ast-opts)
        type-class-ast-opts (assoc ast-opts :ast type-class-ast)
        type-str (emit-type type-class-ast-opts) 
        identifier (when-let [identifer-symbol (get-assignment-identifier-symbol ast-opts)]
                     (str identifer-symbol)) 
        expr-ast-opts (update-in ast-opts [:ast] :init)
        expr (emit expr-ast-opts)
        statement-parts [type-str
                         identifier
                         "="
                         expr]
        statement-parts-opts (-> ast-opts
                                 (assoc :val statement-parts)
                                 map->AnyValOpts)
        statement (emit-statement statement-parts-opts)]
    statement))

(defmethod iface/emit-assignment ::l/curlybrace
  [ast-opts]
  (let [ast (:ast ast-opts)
        type-class-ast (get-assignment-type-class-ast ast-opts)
        complex-expr-opts (let [nested-expr-sub-expr-opts ast-opts
                                ;; expression is nested within AST's :init :expr when calling analyze-ns, and :op = :with-meta
                                ;; but calling analyze returns the expr in AST's :init with corresponding :op
                                assignment-init-expr-opts (if (-> ast-opts :ast :init :expr)
                                                            (-> ast-opts (update-in [:ast] (comp :expr :init)))
                                                            (-> ast-opts (update-in [:ast] :init)))
                                assignment-init-expr-with-type-opts (-> assignment-init-expr-opts 
                                                                        (assoc-in [:impl-state :type-class-ast] type-class-ast))]
                            (cond
                              (-> ast-opts :ast :init) assignment-init-expr-with-type-opts
                              :else nested-expr-sub-expr-opts))]
    (if (is-complex-type? complex-expr-opts)
      ;; emit-assignment-complex-type expects the RHS expr to be in the AST :init key
      (emit-assignment-complex-type (assoc-in ast-opts [:ast :init] (:ast complex-expr-opts)))
      (let [op-code (:op ast)
            type-class-opts (assoc ast-opts :ast type-class-ast) 
            type-str (emit-type type-class-opts)
            identifier (when-let [identifer-symbol (get-assignment-identifier-symbol ast-opts)]
                         (str identifer-symbol))
            expression (emit complex-expr-opts)
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
  {:pre [(or (:env ast-opts)
             (= symb (eval symb)))]}
  (let [ast-opts-env (:env ast-opts)
        symb-class (class symb)
        symb-ast (if (seq ast-opts-env)
                   (az/analyze symb ast-opts-env)
                   (az/analyze symb))
        symb-ast-opts (assoc ast-opts :ast symb-ast)]
    (cond

      ;; emit a "standalone" token
      (= clojure.lang.Symbol symb-class)
      (emit symb-ast-opts)
      
      ;; A seq of symbols -> emit parenthases around the emitted form. Do this for
      ;; an expression like (+ 2 3) -> (2 + 3), but don't do this for a vector
      ;; like [2 3 5] -> (Arrays.asList(2,3,5)).
      (or (isa? symb-class clojure.lang.IPersistentCollection)
          (isa? symb-class clojure.lang.ISeq))
      (cond
        (= 1 (count symb))
        (emit (assoc symb-ast-opts :ast (first symb-ast)))

        (is-complex-type? ast-opts)
        (emit symb-ast-opts)

        :else
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

(defmethod iface/emit-syntactic-operator ::l/curlybrace
  [ast-opts]
  {:pre [(= :static-call (:op (:ast ast-opts)))]}
  (let [ast (:ast ast-opts)
        fn-symbol (-> ast :raw-forms last first)
                                    fn-str (str fn-symbol)
        static-call-fn-symbol (case  fn-str
                                "quot" "/"
                                "rem" "%"
                                ;; Note: extra work is required if supporting Clojure expressions using =
                                ;; with more than 2 expression arguments.  Not really high
                                ;; priority at the moment to support > 2 args for =
                                "=" "=="
                                fn-str)
        arg-strs (emit-args ast-opts)
        expression-parts (interpose static-call-fn-symbol arg-strs)
        expression (string/join " " expression-parts)]
    expression))

(defmethod iface/emit-static-call ::l/curlybrace
  [ast-opts]
  {:pre [(= :static-call (:op (:ast ast-opts)))]}
  (let [ast (:ast ast-opts)
        fn-symbol (-> ast :raw-forms last first)
        fn-str (str fn-symbol)]
    ;; for a :static-call op in the AST, we default to treating it like
    ;; a syntactic operator if not otherwise handled explicitly below
    (case  fn-str
      "get" (emit-get ast-opts)
      (emit-syntactic-operator ast-opts))))

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

(defmethod iface/emit-ns ::l/curlybrace
  [ast-opts]
  {:pre [(= :do (:op (:ast ast-opts)))
         (= "ns" (-> ast-opts :ast :raw-forms first first name))]}  
  nil)

;; metadata

(defmethod iface/emit-with-meta ::l/curlybrace
  [ast-opts]
  {:pre [(= :with-meta (:op (:ast ast-opts)))]}
  (let [analyzed-form-ast-opts (cb-util/unwrap-with-meta ast-opts)
        emitted-form-str (emit analyzed-form-ast-opts)]
    emitted-form-str))

;; entry point

(defmethod iface/emit ::l/curlybrace
  [ast-opts]
  (let [ast (:ast ast-opts)]
    ;; TODO: some multimethod ?
    (case (:op ast)
      :def (case (some-> ast :raw-forms last first name)
             "defn" (emit-defn ast-opts)
             (emit-def ast-opts))
      
      ;; connect this to is-complex-type? somehow
      :map (do
             ast-opts
             (println ":literal? =" (-> ast-opts :ast :literal?))
             (emit-const-complex-type ast-opts))
      [:seq :vector :set :record] (do
                                    ast-opts
                                    (println ":literal? =" (-> ast-opts :ast :literal?))
                                    (emit-const-complex-type ast-opts))
            
      :const (emit-const ast-opts)
      :invoke (case (-> ast :fn :meta :name name)
                "atom" (emit-atom ast-opts)
                "reset!" (emit-reset! ast-opts)
                (emit-invoke ast-opts)) 
      :do (case (-> ast :raw-forms first first)
            'ns (emit-ns ast-opts)
            (emit-do ast-opts))
      :let (emit-let ast-opts)
      :local (emit-local ast-opts)
      :static-call (emit-static-call ast-opts)
      :var (emit-var ast-opts)
      :loop (emit-loop ast-opts)
      :new (emit-new ast-opts)
      :with-meta (emit-with-meta ast-opts)
      :else (cond 
              (:raw-forms ast)
              (emit (assoc ast-opts :ast (-> ast :raw-forms)))))))
