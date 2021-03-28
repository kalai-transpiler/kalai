# Kalai Transpiler

Kalai is a source-to-source transpiler from Clojure to other languages (Rust, C++, Java, ...).

The goal of Kalai is to allow useful algorithms to be encoded once and then automatically be made available natively to other target programming languages.

## Rationale

[Rationale](./docs/Rationale.md)

## Supported forms

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

A formal grammar will be provided in the future.

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

## Documentation

[Rationale](./docs/Rationale.md)

[Design](./docs/Design.md)

[TODO](./docs/TODO.md)

[Contributing](./docs/Contributing.md)

## Development

### Extending or adding languages

To target another language, provide a language specific pass.
See [pass](src/kalai/pass).

### Implementation

See [Design](docs/Design.md).

### Contributing

Issues and Pull requests are welcome!

## License

Copyright © 2020 The Kalai Authors.

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: Unicode License (https://www.unicode.org/license.html).
