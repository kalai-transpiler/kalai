# TODO

* We think we have most of the proof of concept language concepts, but we need to hook up the tests
  - pleasant cider testing
  - workflows (developer and CI)
  - salvaging existing tests
  - indenting strings and comparing ignoring leading space
  - elseif
  - run the output through the native language compiler
  - fix 'lein test' to be command line runnable
* Persistent data structures
  - all languages have a library, but not everyone wants the dependency
  - performance goals
  - would like to support both, choose the mutability that you want
  - need to differentiate (probably by atoms)
  - consider our own macros
  - maps/sets/vectors conj/assoc/update etc
  - import when needed
  - fix data literal tests
* operator and language specific transformation (e.g. = in Clojure is either .equals java or ==)
* temp variable creation and test helper clean up (gensym)
* Fix the annotate AST
* Rust etc
* Expand syslib
* Types!!!
  - Mutable <-- motivation is Rust, is `^:const` good or bad?
  - Generic Types [[]] -> <<>> translation
  - Keyword support
* Start compiling our output files
* "For loops"
* Other concepts?
* Variable casing (when to snake-camel-kebab-case)
* Indentation
* Do we support first class enums?
  - Without it you lose type strictness
  - Namespaced keywords?
  - Would require user declaration form
* Figure out a merge strategy
  - Switch wholesale
  - Backwards compatible (side by side operation)
* Group non-removal in Java for data literals and if then else
* Expand documentation:
  - Preserving knowledge (not tribal)
  - Keeping on track (what did we learn, why did we do that)
  - Today:
