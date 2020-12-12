# TODO

* Types!!!
  - Document target language type conversions (when added)
  - Validate types and narrow the set of accepted types
  - Define common types
    - supporting universals: numeric types should be signed
    - add a pass to convert Java types to target language types
      * depends on choosing another language, too boring in Java
      - Boxed and primitives need to be unified
      - If it's hard to work with both primitives and Boxed,
        we can fall back to only using Boxed
      - Java types equal non-universal - Document target language type conversions (when added)
  - Validate types and narrow the set of accepted types
  - Define common types
    - supporting universals: numeric types should be signed
    - add a pass to convert Java types to target language types
      * depends on choosing another language, too boring in Java
      - Boxed and primitives need to be unified
      - If it's hard to work with both primitives and Boxed,
        we can fall back to only using Boxed
      - Java types equal non-universal types, which is equal types, which is equal to user defined
  - Top level def of data literal needs static block initialization `(def x [1])`
  - Debug why `^Character localDigit (get numberSystemDigits remainder)` transpiles to a type declaration of `Object` but `^{:t :char} localDigit (get numberSystemDigits remainder)` gives the proper type `char` in demo02. Same for `^char sep`.
  - Troubleshoot issue in `getNumberSystemsMap` in demo02 where data literal to be return must have type annotation within a `let` binding else types are missing in Kalai compilation phase (note: also the only nested data structure in demo02)
  - Might be a nice-to-have to propagate function return type to variable identifier in variable assignments, ex: `(def x (fn-call 1 2 3))`
  - Propagate return type of fn to a data literal passed as return value in fn body implementation, ex: `(defn f ^{:t {:mmap [:char :int]}} [] {})`
  - Option types
* Test organization
  - grouping functionality
  - generate docsy from tests
    - new test directory
    - split up into more namespaces
    - don't show the Kalai intermediate syntax
    - markdown inversion
    - GitHub action
    - local action
* Interop
  - Support import statements (ex: for user-defined classes; automatically created when user uses data collections)
  - support String equality comparison properly
  - expand the "function-call" pass (core/interop/kalai/custom)
    - depends on us choosing another target language
  - see if starter code for rust and python works
  - support array types (ex: Java main method)
* Match demo01 and demo02
  - try using `case` instead of `cond` in `getSeparatorPositions` of demo02
  - logic unit tests
* Miscellaneous
  - Update Design doc headings & organization
* We think we have most of the proof of concept language concepts, but we need to hook up the tests
  - workflows (developer and CI)
    * documenting workflows (?)
  - salvaging existing tests
  - figure out coverage testing
  - figure out feature coverage across languages / feature parity
  - indenting strings and comparing ignoring leading space
  - elseif
  - run the output through the native language compiler
  - pleasant cider testing
* Persistent data structures
  - depends on import statements being intelligent (not hard-coded)
  - all languages have a library, but not everyone wants the dependency
  - performance goals
  - would like to support both, choose the mutability that you want
  - need to differentiate (probably by atoms)
  - consider our own macros
  - maps/sets/vectors conj/assoc/update etc
  - import when needed
  - We might be better of using Collection builder functions instead of temporary variables?
  - We should check static block semantics
* Operator and language specific transformation (e.g. = in Clojure is either .equals java or ==)
* test helper clean up
  - don't report failures twice
* Rust etc
* Support function calls where functions are defined in input code across namespaces
  - Solve importing
* Allow users to bring their own functions
* Allow users to bring their own languages
* Indentation
* Do we support first class enums?
  - Without it you lose type strictness
  - Namespaced keywords?
  - Would require user declaration form
* Expand documentation:
  - Preserving knowledge (not tribal)
  - Keeping on track (what did we learn, why did we do that)
