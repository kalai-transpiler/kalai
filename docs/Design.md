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

```
(defn mess [^{:t :int
              :mut true
              :ref true} a]
  (let [^{:t {:vector [int]}} z (atom ^:mut [3 4])
        ^{:t :int} i (atom 0)
        ^{:t :int} j a]
    (swap! z conj 5)
    (swap! z conj 6)
    (count @z)))
```
    
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

In Clojure you can type hint and metadata let symbols,
but not if they bind primitive values.

Aggregate types will be composed of "primitive types" (types that are defined in Kalai as universal across languages).
Doing so follows Clojure's data simplicity principle: don't complext plain data with types.
To support new concepts (for example StringBuffer), users will need to add to the Kalai supported types and implement code for each of the target languages.
We should minimize the effort required from users to extend Kalai, which would be done through user supplied data/functions.

Types can be supplied as either type hints (which is metadata `{:tag type}`),
or Kalai specific metadata `{:t type}`.
Kalai specific metadata is necessary because
1. Number literals in Clojure are strictly longs and doubles,
   and cannot be typehinted as ints/floats.
   But these are useful types for many target languages.
2. Representing generic types

You should prefer specifying a `{:t type}` over typehints.

The canonical representation of types in Kalai s-expressions is keywords.

Generic types, also known as parameterized types (including collection types)
are represented as a map containing a single key value pair:
`{:map [:long :string]}`
where the key is the parent type (generic type)
and the value is the type parameters (child types).
This notation is sufficient to represent the tree like nesting of types,
Information about the type nodes is captured in metadata if required,
which enables us to use the simple structure.
`(def ^{:t '{:map [:long ^:mut ^:opt :str]}} x)`

We provide a type aliasing feature:
1. `(def ^{:kalias {:map [:long :string]}} Z)` defines an alias Z which will not exist in the final output.
2. `(def ^{:t Z} x)` uses the Alias Z, the final output will replace Z with `{:map [:long :string]}`.
Being equivalent to `(def ^{:t {:map [:long :string]} x)`.

TODO: maybe using the :t convention is redundant...
annotating the types doesn't conflict, but it adds noise and is easy to forget
maybe allow ^{:map [:int :int]}

Invariants:
1. a `{:t type}` will never be replaced
2. a `{:t type}` will always be a keyword (or generic type)
3. The `:tag` and `:o-tag` will not be used in s-expressions after the ast annotation,
   any inference of `:t` from `:tag` or `:o-tag` will occur converting ast to s-expressions.
   Only `:t` will be used in the s-expressions.
   There is an exception, which is interop: certain tags may be used to identify method calls on things like StringBuffers.

Unfortunately, both Clojure and tools analyzer do not resolve symbols
in meta data on arglists or bindings.

    (defn f ^{:t ZZZZa} [a b] a)
    ;;=> #'kalai.core/f
    (def ^{:t ZZZZa} x 1)
    ;;=> Unable to resolve symbol: ZZZZa in this context
    (type (:tag (meta (second '(def ^Integer x)))))
    => clojure.lang.Symbol

So we have to rely on those being identifiable by declaration.

### Type propagation

We propagate metadata in 3 different ways, which we're calling:

* initial value to binding
* binding to initial value
* bindings to locals

#### bindings to locals

Here, we propagate type metadata from the binding to all references in scope,
according to the static analysis given to us by tools.analyzer.
The reason we do this is that tools.analyzer gives us an accurate snapshot of the
environment at every node of the AST, but it doesn't collect the metadata we need.
We need the type information.
We don't use tools.analyzer's type information because we give users full control
to use the custom `{:t type}` form instead of `^type`.
For example if you want to put a type on a collection literal that has the full
information needed for output to a statically typed language then you need more
information than Clojure/JVM type erasure allows.

        (let [^{:t {:vector [int]} v []]
          (println v))

We propagate type information which is stored in metadata (`^{:t {:vector [int]}`)
from the place where they are declared on a symbol (`v`)
to all future usages of that symbol in scope (`(println v)`).
When the type metadata is not provided and the type of the
initial value is known, we use the type of the value.



#### binding to initial value

The second way that we propagate type information is inside an initialization
or assignment statement.
We allow metadata propagation in an initialization statement to be a convenience that avoids repeating the type in both the binding and value:

    (let [^{:t {:vector [int]}} v []])

Compare that with Java where there is redundancy in syntax:

    ArrayList<Integer> v = new ArrayList<>();

In the Kalai input code, the type on `v` is propagated to the untyped vector literal initial value.
This allows the temporary variables used in the data literal expansion to have the proper types.

#### initial value to binding

However, we want to additionally support the case when the user provides type information on the initial value of an assignment but not the binding. Ex:

    (let [v ^{:t {:vector [int]}} []])

Currently, we perform binding-to-local metadata propagation within the AST phase of the pipeline. That is because there is more nuance. For example, if you put a type on the vector

    (let [v ^{:t {:vector [int]}} []
          v2 v])

Here, the `v2` binding will have the type info (via metadata) of `v` propagated to it. So the propagation of type info metadata from the initial value to the binding (`^{:t {:vector [int]} []` to `v`) should before the propagation from the binding to the local (`v` to `v`) so that `v2` can have `v`'s metadata propagated to it.



#### ordering of propagation types

As said before, propagation of metadata from bindings to locals happens must be done in the AST using tools.analyzer. Also, propagation from initial values to bindings must happen before propagation from bindings to locals. Therefore, propagation from initial values to bindings must also be done in the AST.

Since two of the three ways in which propagation happens are done in the AST, for code reuse / simplicity purposes, we have all 3 ways done in the AST.

##### potential ordering problem and potential solution

However, there is a problem that is not fully solved when it comes to the ordering of the execution of these type propagation passes.  In this example

    (let [v ^{:t {:vector [int]}} []
          v2 v])

We have type info propagating as follows:

1. from the initial value (`^{:t {:vector [int]} []`)to the binding (`v`)
2. from the binding (`v`) to the local (`v`)
3. from the initial value (`v`) to the binding (`v2`)

Step 1 and Step 3 must happen one after the other, but they are both the same operation ("initial to binding propagation").

This problem is even more obvious when you have

    (let [v ^{:t {:vector [int]}} []
          v2 v
          v3 v2
          v4 v3])

And you try to get type info for `v3` but it hasn't propagated from `v` beyond `v2`.

However, in this example, when we look at the binding line `v4 v3`, and we see `v3`'s binding (`v3 v2`) also has no type info specified, we need to continue recursively. And tools.analyzer contains all this information at the node for `v4`'s binding in a nested manner in the environment. If we follow this binding info recursively within the nested environment info, we should eventually get the type (`^{:t {:vector [int]}}`) or throw an error. Once we do that, our recursive navigation for getting the type will subsume/replace the work done in binding-to-local propagation entirely within this initial-value-to-binding propagation step.

In this new restructuring of type propagation phases, we think that the ordering of initial-value-to-binding and binding-to-initial-value propagation does not matter because binding-to-initial-value propagation is a convenience that only applies when the initial value is a data literal. For example, if we don't have a data literal, but we do have type information:


    (let [v ^{:t {:vector [int]}} []
          v2 v      
          ^{:t {:vector [int]}} v3 v2
          v4 v3])

then we get the type of `v4` from `v3`. If we need the type of `v2`, we can get it from `v`.

And when we do have a data literal initial value:

    (let [v ^{:t {:vector [int]}} []
          v2 v      
          ^{:t {:vector [int]}} v3 []
          v4 v3])

then getting the types for `v4` and `v2` are unaffected, and we have all the info we need for `v3`'s binding (that is to say, propagate from binding to value, which enables the info we need to generate the full assignment statement in the target language).


#### precedence

User defined types on target will not be replaced by type propagation.

    (let [^{:t :double} x 1])

Here x keeps its type double, the type of x is not replaced by the type long of 1.


## Notes

Turning data literals into s-expressions, cannot use data literals in intermediate s-expressions.
groups of statements in place of expressions are raised to above the statement,
assigned to a temp variable in scope.
Similar to return (identifying statements) but different... context can be in the statement, and statements can have child statements.

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

Operators have a further complication;
in Clojure everything is a function,
so it is possible to write a higher order function,
and pass an "operator".
Operators as values will need to be replaced with wrapper functions,
either through provided interop or by the user.

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
before the current expression statement,
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
