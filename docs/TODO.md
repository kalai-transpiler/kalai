# TODO

* Types!!!
  - define common types
    - supporting universals: numeric types should be signed
    - add a pass to convert Java types to target language types
      * depends on choosing another language, too boring in Java
      - Boxed and primitives need to be unified
      - If it's hard to work with both primitives and Boxed, we can
        fall back to only using Boxed
  - Generic Types [[]] -> <<>> translation
    - we added <<>> but data literal transformation intercepts
    - the nested vector notation
  - Mutable
    - in a let binding
      * depends on choosing Rust as our next target language
    - mutable data literals (see test t3-2)
  - Aliasing
* Interop
  - expand the "function-call" pass (core/interop/kalai/custom)
    * depends on us choosing another target language
  - see if starter code for rust and python works
  - implementing this probably requires implementing support for
    a second target language to ensure we are not doing Java
    specific things  
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
* operator and language specific transformation (e.g. = in Clojure is either .equals java or ==)
* test helper clean up
  - don't report failures twice
* Fix the annotate (type annotation performance due to matching vs AST node tree walking) AST
* Rust etc
* Support function calls where functions are defined in input code across namespaces
  - Solve importing
* Allow users to bring their own functions
* Allow users to bring their own languages
* Expand syslib
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
