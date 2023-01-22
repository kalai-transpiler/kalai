# Rationale for Kalai

What features of Clojure would you take with you, 
if you couldn’t write Clojure?
Other languages have only adopted bits and pieces of the Clojure way of programming.
We wrote a transpiler to bring that paradigm over to the other languages in order to write libraries that work the same.
Our experiences taught us how differences in the destination languages affected variations in certain details,
and the relative impact of the foundations necessary to support Clojuresque solutions.

## The core idea

* Libraries that could compile to multiple languages
  - SQL builder
  - i18n algorithms
  - Data diffing
* The essence of general purpose programming
  - What are the concrete outcomes of selecting a programming language?
  - Can styles of programming be transferred from one language to another?
  - Is transpiling a way to make the Clojure way accessible?
    * Are persistent data structures enough? (spoiler alert, no)
    * Accompanying functions, and values
* AST s-expressions and metadata

## Why does Kalai exist?
Write once compile everywhere

### Cross language libraries
* We use them all the time:
  - REST apis
  - Internationalization
  - SQL builder
* The more you put in, the more valuable

### Performance and size
* Native binaries
* Dead code elimination when used by your own downstream applications

## What do I get from Kalai?
### The value of data literals
### The value of data oriented programming

## Why is Clojure a good source language?
included in [Why Clojure is good for transpilers](https://elangocheran.com/2020/03/18/why-clojure-lisp-is-good-for-writing-transpilers/)


## What are the other options
* J2OBJC, J2CL, 1:1 translations
* Haxe 1:many from OCaml

[See Design](Design.md)

## Tradeoffs

### Dynamism/REPL

We lose REPL / interactive development style in order to gain cross-lang/platform reach 

