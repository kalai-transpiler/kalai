# Syntax of Universal Language for Kalai
<!-- https://xkcd.com/927/ -->

## Overview

Kalai supports the majority of Clojure language constructs.

Namespaces translate to classes,
functions translate to static functions,
defs and lets translate to variables,
atoms translate to mutable data structures,
data literals default to equivalent persistent data structures via libraries when used.

Kalai expressly disallows top-level forms other than `defn` and `def`.
For example:

```clojure
(ns foo.bar)
(println "hi")
```

While valid in Clojure,
most target languages disallow code execution during compilation,
so Kalai will reject this code.

* Certain languages have requirements that are narrower than other languages
* Supporting all languages requires supporting the narrowest requirement

Ex: statically typed languages requires type annotations for new identifiers

## Requirements in order to support all target languages

### Types

* Need type info on variables due to statically typed languages as target languages
* Functions that return sizes are limited to integer sizes despite target languages supporting
abstract concepts of size. Specifically, integer size = a Java integer = 32-bit signed. All
  Java integers return integer sizes of collections, strings, etc. A language like Rust uses `usize`
  for a platform dependent size, which must be cast to `i32`, `u32`, `i64`, `u64`, etc. to match the
  type specified by the user's input code.
* A language like Rust does not allow floating point types in sets or as the keys of maps. 
  Therefore, when outputting to such languages, instead of having a `^{:t {:set [:float]}}`, we must use a `^{:t {:set [:any]}}`, and then subsequently cast elements that we get (which would be of type `:any`) to the desired primitive type (`:float`). 
  
### Keywords

* Keywords are treated as strings in the target language.
We can't use them as functions nor check whether a value is a keyword using `keyword?`.
* The decision is based on the lack of benefit in the target languages, especially in a context of transpiling to the target languages.
Keywords had/have 2 main benefits in Clojure: 1) interning so that only a single instance is stored, and 2) using keywords as functions (mainly for lookups in first position, but also for HOFs (ex: `map`).
  But subsequent versions of Java now intern string literals, and the functions-in-first-position syntax only benefits Clojure/Lisp.

### Switch / Match

* Rust requires default branch (arm) on a match expression
  * Default is required when arm case values do not exhaustively cover the full space of values for the type
* Java requires the switch argument to be a primitive or String or Enum (?)

### Functions / Overloads / Arities

* Rust requires no overloaded functions
  - A function name can only be used once -- like C, not like C++/Java
  - So we disallow overloaded fns in Clojure input (aka a "multi-arity fn" in Clojure) 

## Functionality omissions due to current lack of need 

### Enums (or lack thereof)

### Arrays (or lack thereof)

### Try / catch

* May not be possible to support
  * Because Rust has panics and Results

## Gaps in target language support filled in by us to match Kalai/Clojure expressiveness

### If statements as expressions

* We support this in Java with some extra work (using groups)
* Rust will not compile when conditionals as expressions don't have
  an "else" branch (that is, only has a "then" branch)
  - Therefore, we only support conditionals as expressions with the "else" branch, too
  - We do not validate the input for having the "else" branch, and therefore rely on the target language compiler(s) downstream to throw errors to the user 
  
### Cond expressions

* Default cases must only use a keyword (ex: `:else`, `:default`, or any other keyword). Do not use literal `true`.

### Data literals for collections

* Languages like Java don't have any collection literal syntax
* Languages like Rust have literal syntax for some but not all collections
  * Rust: vectors -> `vec!`, but not for set and map)
    C++: can support literal values in initialization statement only, for some versions of C++ and later only

### Immutability and persistent data collections

* Clojure makes values immutable by default, uses persistent data collections
  - Persistent collections implies immutable. Clojure persistent collections are heterogeneous
  - Heterogenous collections allows easy nesting of data structures
* Persistent data collections in target languages are supported via 3rd-party libraries
  - Ex: Bifurcan in Java, rpds in Rust
* Types
  - There are separate types for mutable and immutable collections in Kalai (Ex: `:mvector` is a mutable vector, `:vector` is an immutable (persistent) vector).
  
#### Sequences
  
* Clojure uses the `seq` abstraction/interface for many core library functions, and implements it on all collection types
  - Clojure seqs are immutable
* Other languages may have something analogous, but often they are one-use only (perhaps because they are mutable)
  - Ex: `Stream` in Java, `Iterator` in Rust
* In Kalai, we may represent internally such seq-like constructs using a type (ex: the type map `{:t :seq}`)
  - However, we currently are not supporting users of Kalai to create local binding values out of seqs
    * This is partly due to analogous target langauge constructs being one-use only
    * If there is a need, we can revisit, with the restriction that it only really makes sense / is useful when computed from persistent collections.
  
## Gaps in target languages that Kalai will not fill

### Apply

`apply` is a Clojure language feature that is prohibitively challenging many other target languages do not support.
Most usages of `apply` can be replaced by `reduce`.

Ex: `(apply + numbers)` then becomes `(reduce + numbers)`