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

(defn mess [^{:t :int
              :mut true
              :ref true} a]
  (let [^{:t {:vector [int]}} z (atom ^:mut [3 4])
        ^{:t :int} i (atom 0)
        ^{:t :int} j a]
    (swap! z conj 5)
    (swap! z conj 6)
    (count @z)))
    
Even though we apply the word mutable to bindings and to data collections
it is tricky to recognize that the opposite of a mutable binding
is a constant.
The opposite of an mutable collection is a immutable collection,
but a persistent one implies extra functionality above immutability.
The default is persistent.
Annotating ^:mut on a collection gives you a mutable collection.
You cannot annotate ^:mut on non-collection literals such as numbers
or strings, both because Clojure does not allow it, and because
it doesn't make sense in Kalai either.
When it comes to annotating types in Kalai,
we know that we have the type and the mutability to annotate.
C++ and Java are examples of this.
Because of Rust we also have to annotate whether it is a reference type.
C++ might be similar to Rust in this regard.
If you want to pass by mutable ref, you should pass an atom,
so that the Clojure code behaves the same as the target language code.
You can still pass a non-mutable ref (for Rust) in which case you
shouldn't pass an atom.
Mutability correlates with atoms.
Use of deref is to make sure Clojure behavior matches target language behavior,
Metadata, including type hints, is to make sure that target language
syntax is complete and compiles and works.
A mutable pass by reference implies reference.
In other words, in the metadata map, keyword mut true implies that
keyword ref is true, but there is no inverse implication, as
ref can be true while mut is false.

```rust
let mut m = HashMap::new();
&m.insert(k, v);
printer_fn(&m);

pub printer_fn(m: &HashMap) {
   println!("{}", m);
}
```

```clojure
(defn printer-fn [^:ref ^{:t :map} m]
  (println m)
  ...)

(defn fn2 [^:ref ^:mut ^{:t :map} m]
  (println @m)
  ...)

(defn printer-fn3 [^{:t :map} m]
  (println m)
  ...)

;; m should have ^:mut on it
(let [m (atom ^:mut {})]
  (swap! m assoc k v)
  (printer-fn @^:ref m)

  (printer-fn @^:ref m)
  (fn2 ^:mut ^:ref m)
  (printer-fn3 @m))
```



## Strategy for types, alias, metadata

## Notes

In Clojure you can type hint and metadata let symbols,
but not if they bind primitive values.

Aggregate types will be composed of "primitive types" (types that are defined in Kalai as universal across languages).
Doing so follows Clojure's data simplicity principle: don't complext plain data with types.
To support new concepts (for example StringBuffer), users will need to add to the Kalai supported types and implement code for each of the target languages.
We should minimize the effort required from users to extend Kalai, which would be done through user supplied data/functions.

Types can be supplied as either type hints (which is metadata `{:tag type}`),
or Kalai specific metadata `{:t type}`.
Kalai specific metadata is sometimes necessary because
1. Number literals in Clojure are strictly longs and doubles,
   and cannot be typehinted as ints/floats.
   But these are useful types for many target languages.
2. Representing generic types 

Types can be supplied as Java types, classes, keywords or symbols.
Keywords are convenient to work with.
Types are accepted as presented, and left to the final stage in
the language specific pipelines to be converted where necessary.
Kalai makes no attempt to validate the types at present.

Generic types, also known as parameterized types (including collection types)
are represented as a map containing a single key value pair:
`{:map [:long :string]}`
where the key is the parent type (generic type)
and the value is the type parameters (child types).
This notation is sufficient to represent the tree like nesting of types,
Information about the type nodes is captured in metadata if required,
which enables us to use the simple structure.
`(def ^{:t '{kmap [klong ^:mut ^:opt kstr]}} x)`

We provide a type aliasing feature:
`(def ^{:t Z} x)` => In the AST, replace Z with the value of Z => `(def ^{:t {:map [:long :string]} x)`
`(def ^{:kalias _} Z)` => In the AST, remove the def (don't emit it)


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

Symbols like `inc` resolve to vars like `clojure.core/inc`.
Therefore we need to annotate them in the AST,
so that when replacing function calls with target language equivalents,
we replace the right things (not some locally scoped name).

## Truthiness

Hope types save us! Wrap boolean around things we don't know

## Equality

== .equals (but needs to be nil safe)

Match a single group inside an s-expression:

    (!before ... (group . !tmp-init ... ?tmp-variable) . !after ...)

Match all the groups inside an s-expression:

    ((m/or (group . !tmp-init ... !tmp-variable)
           !tmp-variable) ...)

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
