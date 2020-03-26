# Kalai Transpiler

Kalai transpiler is a source-to-source transpiler to convert Clojure to multiple target languages (Rust, C++, Java, ...).

The goal of Kalai is to allow useful algorithms to be encoded once and then automatically be made available natively to other target programming languages.

## Rationale

See [why Clojure is good for writing transpilers](https://elangocheran.com/2020/03/18/why-clojure-lisp-is-good-for-writing-transpilers/).

## Usage

### Supported forms

The forms that are currently supported are listed in [`interface.clj`](./src/kalai/emit/interface.clj).

### Example

If you have code written in `your.namespace`, then you can emit code as follows, assuming there is a file `your/namespace.clj` relative to where the program is run:

```clj
lein run -i your -o someoutdir -l rust
```

In this example, an output file will be written to `someoutdir/your/namespace.rs`.

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

## License

Copyright © 2020 The Kalai Authors.

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: Unicode License (https://www.unicode.org/license.html).
