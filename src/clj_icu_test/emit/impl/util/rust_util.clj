(ns clj-icu-test.emit.impl.util.rust-util)

(defn is-assignment-expr-atom
  "Input is assignment expression AST, not full assignment AST. Returns whether expression AST is defining an atom"
  [ast]
  (boolean
   (and (= :invoke (-> ast :op))
        (= "atom" (-> ast :fn :meta :name name)))))
