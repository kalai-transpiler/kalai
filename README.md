# Kalai Transpiler

Kalai transpiler is a source-to-source transpiler to convert Clojure to multiple target languages (Rust, C++, Java, ...).

The goal of Kalai is to allow useful algorithms to be encoded once and then automatically be made available natively to other target programming languages.

## Rationale

See [why Clojure is good for writing transpilers](https://elangocheran.com/2020/03/18/why-clojure-lisp-is-good-for-writing-transpilers/).

## Supported forms

The forms that are currently supported are listed in [`interface.clj`](./src/kalai/emit/interface.clj).

## Usage

If you have code written in `your.namespace`, then you can emit code as follows, assuming there is a file `src/your/namespace.clj` relative to the current directory:

```clj
lein run -i src/your/namespace.clj -o someoutdir -l rust
```

In this example, an output file will be written to `someoutdir/your/namespace.rs`.

From the root directory of this project, you can run

```
lein run -i examples/a/demo01.clj -o out -l rust
```

creates `out/examples/a/demo01.rs`

```
lein run -i examples/a/demo02.clj -o out -l java
```

creates `out/examples/a/demo01.java`

### Demo unit test cases

Example demo 1 has input code at [`test/kalai/demo/demo01.clj`](test/kalai/demo/demo01.clj) and emitter tests at [`test/kalai/demo/demo01.clj`](./test/kalai/demo/demo01_test.clj).

Example demo 2 has input code at [`test/kalai/demo/demo02.clj`](test/kalai/demo/demo02.clj) and emitter tests at [`test/kalai/demo/demo02.clj`](./test/kalai/demo/demo02_test.clj) and logic tests at [`test/kalai/demo/demo02_logic_test.clj`](./test/kalai/demo/demo02_logic_test.clj).

### Implemented target languages

- Rust
- C++
- Java
- Clojure (Kalai is source compatible with Clojure)

See also `kalai.emit.langs/TARGET-LANGS`

## Development

### Extending or adding languages

Clojure supports namespaced keywords to enable the dynamic dispatch fallback hierarchies for multimethods.
The namespaced keywords for the target languages follow the Clojure derivation tree:

- `::l/curlybrace` ("curly brace" languages)
  * `::l/rust` (Rust)
  * `::l/cpp` (C++)
  * `::l/java` (Java)

To extend or add implementations, add multimethod definitions in `kalai.emit.impl/mylang.clj`.

### Contributing

Issues and Pull requests welcome!

### Implementation strategy

Multiple passes:
* Leverage tools analyzer to parse and emit canonical forms
* Pattern match for concepts we support
* Language specific ast converter
* Condense and beautify
* Stringification

## License

Copyright © 2020 The Kalai Authors.

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: Unicode License (https://www.unicode.org/license.html).

## Notes

You CAN type hint and metadata let symbols,
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

## TODO!!!!

* We think we have most of the proof of concept language concepts, but we need to hook up the tests
  - want to use expectations but cider has no notion of expectations
  - what granularities are we using
  - workflows (developer and CI)
  - refactoring test
  - salvaging existing tests
  - working with big strings... is that really what we want?
* Rust etc
* Expand syslib
* Types!!!
  - Mutable <-- motivation is Rust, is `^:const` good or bad?
  - Generic Types [[]] -> <<>> translation
* Start compiling our output files
* "For loops"
* Other concepts?
* Variable casing (when to snake-camel-kebab-case)
* Indentation
