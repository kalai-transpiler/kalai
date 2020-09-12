# Documentation

## Overview

Multiple passes occur to incrementally transform Clojure to the target representations.

* Leverage tools analyzer to parse and emit canonical forms
* Pattern match for concepts we support
* Language specific ast converter
* Condense and beautify
* Stringification

## Strategy
### Nano pass
### ASTs vs s-expressions
* The preference for operating on s-expressions
### Hierarchy

### Leveraging data-oriented, declarative tools

#### Clojure
* Language
* Ecosystem
* Simplicity
* Everything works together
* See [why Clojure is good for writing transpilers](https://elangocheran.com/2020/03/18/why-clojure-lisp-is-good-for-writing-transpilers/)

#### Rewriting (meander)
* Declarative
* Visual
* Concise

#### Analyzer (macroexpand, normalize)

## The pipeline passes

### Dependencies and assumptions
* Pass order is important
* Linear is easiest!
  - shared passes seem tricky, they bit us once already

### what does each pass do,

### what does it assume,
### why does it exist

## Return statement insertion strategy

## The group s-expressions
* What does "group" mean, how are we using it?
* How return statements interact with groups
* Data literals in init statements in Java (example of groups)
* Constructs in Clojure that have side-effects and return values (eg swap! interacting with groups)

## Recursion
* happens at every level, on all expressions
* should not happen on everything in the AST (maybe use az/walk when necessary)
* style of recursion for compiler is different:
  - mutual recursion
    - entry point for each transformation
    - branches that need to be checked to perform a transform
    - children of that node get fed back into the entry point
    - expressions and statements behave like entry points but relate to each other
  - each pass changes the data, but doesn't call directly down to stringify
  - just spitting out transformations, not the final product (see Nano pass)

## Statements/expressions
    
## Strategy of mutable vs immutable
## Strategy for types, alias, metadata

## Notes

In Clojure you can type hint and metadata let symbols,
but not if they bind primitive values.

Aggregate types will be composed of "primitive types" (types that are defined in Kalai as universal across languages).
Doing so follows Clojure's data simplicity principle: don't complext plain data with types.
To support new concepts (for example StringBuffer), users will need to add to the Kalai supported types and implement code for each of the target languages.
We should minimize the effort required from users to extend Kalai, which would be done through user supplied data/functions.
We could provide a type aliasing feature:
`(alias Z [kmap [klong kstring]])` => (def ^:kalai-alias Z ...) => In the AST, remove the def (don't emit it)
`(def ^{:t Z} x)` => In the AST, replace Z with the value of Z => `(def ^{:t [kmap [klong kstring]] x)`

`(def ^{:t '[kmap [klong ^:const ^:opt kstr]]} x)`
Notes on type names: don't want them to collide with Clojure words or user expectations of target language names.
They must be quoted.
Collection types go in nested vectors.

Turning data literals into s-expressions, cannot use data literals in intermediate s-expressions.
groups of statements in place of expressions are raised to above the statement,
assigned to a temp variable in scope.
Similar to return (identifying statements) but different.. context can be in the statement, and statements can have child statements.
