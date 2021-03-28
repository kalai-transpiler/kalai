# Kalai Transpiler

Kalai is a source-to-source transpiler from Clojure to other languages (Rust, Java, C++, ...).

The goal of Kalai is to allow useful algorithms to be encoded once and then automatically be made available natively to other target programming languages.

## Rationale

[Rationale](./docs/Rationale.md)

## Supported forms

Kalai is designed to operate on working Clojure source code.
Kalai does not introduce any new syntax on top of Clojure.
Kalai supports a sufficient subset of Clojure language constructs to represent many useful algorithms and applications.

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

Read more about supported forms in [docs/Syntax.md](./docs/Syntax.md).

### Implemented target languages

- Rust
- Java

See also `kalai.emit.langs/TARGET-LANGS`

## Usage

The easiest way to get started is to follow the pattern established in the `examples` folder.
The [examples/deps.edn](./examples/deps.edn) defines how you can invoke Kalai.
You can replace `{:local/root ".."}` version with `{:mvn/version "<INSERT VERSION HERE>"}` if you wish to rely on a release version.

The [examples/Makefile](./examples/Makefile) defines tasks to invoke Kalai and downstream compilers.

### Setup to run examples

You will need to install the following tools:

- `clojure` command-line tool (from the [official Clojure distribution](https://clojure.org/guides/getting_started), ex: `brew install clojure/tools/clojure
  ` on macOS)
- Make (simple commands to run transpile+compile examples)
- Gradle (compile Java transpiled code in examples)
- Cargo (compile Rust transpiled code in examples)

### Running examples

Start in the `examples` directory:

```
cd examples
```

To transpile the examples and invoke the downstream compilers:

```
make
```

To only invoke the Kalai transpiler:

```
make transpile
```

If you don't want to use Make, you can invoke Kalai using the Clojure CLI:

```
clojure -M -m kalai.exec.main --src-dir src/main/clj --verbose
```

To run the compiled output:

`./rust/target/debug/demo_01`


## Development

Pull requests welcome! See [Contributing](./docs/Contributing.md).

Planned work is sketched out in [TODO](./docs/TODO.md).

### Extending or adding languages

To target another language, provide a language specific pass.
See [Design](docs/Design.md) and [pass](src/kalai/pass).


## License

Copyright © 2020 The Kalai Authors.

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: Unicode License (https://www.unicode.org/license.html).
