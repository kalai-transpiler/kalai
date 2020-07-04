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


## TODO!!!!

* Types!!!
* Extend the canonical ast to support loop/while etc
* Start compiling our output files
* Indentation
* Variable casing
* We have a recur hanging out in our loop
* Side-effect expression statements are not detected (connect nested statement forms)
