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
      - Java types equal non-universal types, which is equal to user defined
  - Top level def of data literal needs static block initialization `(def x [1])`
  - Might be a nice to have to propagate function return type to variable identifier in variable assignments.
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
  - expand the "function-call" pass (core/interop/kalai/custom)
    - depends on us choosing another target language
  - see if starter code for rust and python works
* Match demo01 and demo02
  - logic unit tests
* We think we have most of the proof of concept language concepts, but we need to hook up the tests
  - pleasant cider testing
  - workflows (developer and CI)
  - salvaging existing tests
  - figure out coverage testing / feature parity
  - indenting strings and comparing ignoring leading space
  - elseif
  - run the output through the native language compiler
* Persistent data structures
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
* Figure out a merge strategy
  - Switch wholesale
  - Backwards compatible (side by side operation)
* Expand documentation:
  - Preserving knowledge (not tribal)
  - Keeping on track (what did we learn, why did we do that)
