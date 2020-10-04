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

When using data literals of sets and maps, the ordering of output statements
may not necessarily correspond to the ordering of elements in the source code,
due to Clojure's reader interpreting data literals before any other library or tool
or our code can see it.

When writing tests note that the output ordering might change,
even if you just append items to the end of the input.
This is due to our use of the default Clojure reader which parses data literals
into maps and sets with no inherent order.
This ordering should be consistent.


## Keywords

Keywords are treated as strings.

When using keywords as functions, there are some caveats:
Convert to get, which is only defined for maps.
(We could provide set specific interop mappings for get in the future).
Do not use with sets, instead use `contains?`.
Use contains if you want a boolean.


## Mappings (function-call.clj)

This part of the pass pipeline considers "function call"
as defined in terms of what most target languages think of.
For example: println
For anti example: + - * / are functions in Clojure but are operators in target languages.
We adopt the Clojure view that constructors and methods are functions.
Methods are functions of objects with args.
Constructors are static functions.

  * Core core
    - println
  * Kalai provided things
  * OOP (Interop)
    - constructors and methods
  * User provided things!

Operators are kinda different kinda similar (constructs), part of the syntax

Truthiness:
Hope types save us! Wrap boolean around things we don't know

Equality:
== .equals (but needs to be nil safe)

Match a single group inside an s-expression:
    (!before ... (group . !tmp-init ... ?tmp-variable) . !after ...)

Match all the groups inside an s-expression:
    ((m/or (group . !tmp-init ... !tmp-variable)
           !tmp-variable) ...)

If statements bubbling:

# Patterns

When we have an input form that represents an expression,
but must be written in terms of multiple forms,
we put that collection in a group,
in order to have one and only one return value.
However, because that group is in an expression position,
we should only have one form, which should be an expression form.
In order to achieve that we move all but the last form to
preceed the current expression statement,
declare a temp variable to store the result of the initialization,
and put the temp variable in the original expression position.
To do create the temp variable, we have a gensym2 function that looks like:

    (def c (atom 0))
    (defn gensym2 [s]
      (symbol (str s (swap! c inc))))
      
The auto-increment on the gensym2 suffix enables deterministic tests.

We create the group of forms that the input expands to,
then we run the raise-forms pass to separate the return expression
in the group from the preceeding initialization statements in the group.
We require that the very next pass must flatten groups (remove any remaining).

In choosing how to support fn invocations to interop methods
  - 2 options:
    * make users use Clojure's Java interop in the input code
    * create wrapper fns like in the current impl
  - What about cases where there are many overrides (ex: StringBuffer.insert()) ?
    * this seems like an argument for defining interop in the input source code using Java interop
    * what if there are operations that the Java APIs don't support?
      (hypothetically: what if there should be a StringBuffer.insert for a DateTime object)
      -> we can always reserve the right to create a wrapper fn and have ppl transpile
  - What is important in a syslib?
    * IO (files/streams/reading/writing)
    * StringBuffer
    * Dates and date manipulation
  - What is important for client libs?
    * sockets, http, connections, listening
    * database clients
    * UIs (too hard!)
  - How do users create mappings?
    * provide a function (a Meander rewrite rule) that converts s-expressions

We are mapping target language s-expressions (usually Interop or Clojure core functions) to target language syntax s-expressions
