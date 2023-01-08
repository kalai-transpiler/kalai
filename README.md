# Kalai Transpiler


Kalai is a source-to-source transpiler from Clojure to other languages (Rust, Java, C++, ...).

The goal of Kalai is to allow useful algorithms to be encoded once and then automatically be made available natively to other target programming languages.

<img src="kalai-logo.png" alt="Kalai" width="200" style="float: right"/>

Kalai (கலை) means "art" in Tamil.



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

[![Clojars Project](https://img.shields.io/clojars/v/com.github.echeran/kalai.svg)](https://clojars.org/com.github.echeran/kalai)

The easiest way to get started is to follow the pattern established in the `examples` folder.
The [examples/deps.edn](./examples/deps.edn) defines how you can invoke Kalai.
The [examples/Makefile](./examples/Makefile) defines tasks to invoke Kalai and downstream compilers.
The examples are described in the "End-to-end toolchain usage" section.

### Setup to run Kalai

You will need to install the following tools:

- `clojure` command-line tool (from the [official Clojure distribution](https://clojure.org/guides/getting_started),
  ex: `brew install clojure/tools/clojure` on macOS)

### Running Kalai to transpile your own code

In order to run Kalai on your input Clojure sources to get output sources in your preferred target programming langauges,
you will need to provide the following:

- input source and output source directories
  
```shell
mkdir myproject
cd myproject
mkdir src
mkdir out
```

- create a `deps.edn` with the following contents

```clojure
{:deps {com.github.echeran/kalai {:local/root ".."}}}
```

You should replace `{:local/root ".."}` version with `{:mvn/version "<INSERT VERSION HERE>"}` in order to rely on a release version.

- create your input source code: `src/mynamespace/simple.clj`

```clojure
(ns mynamespace.simple)

(defn add ^Long [^Long a ^Long b]
      (+ a b))
```

- invoke the tool

```shell
clojure -M -m kalai.exec.main --verbose --src-dir src --transpile-dir out
```

For more options, you can run help with

```shell
clojure -M -m kalai.exec.main --help
```

## End-to-end toolchain usage

Kalai transiples from Clojure to your target language(s).
The code in `examples` show how to compile your target language code, using Makefiles,
in order to invoke a complete end-to-end process.


### Setup to run provided examples

You will need to additionally install the following tools:

- Make (simple commands to run transpile+compile examples)
- Gradle (compile Java transpiled code in examples)
- Rustup (install Rust compiler, Cargo, etc.)

### Running provided examples

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
clojure -M -m kalai.exec.main --verbose
```

To run the compiled output:

`./rust/target/debug/demo_01`

## Development

Pull requests welcome! See [Contributing](./docs/Contributing.md).

Planned work is sketched out in [TODO](./docs/TODO.md).

### Extending or adding languages

To target another language, provide a language specific pass.
See [Design](docs/Design.md) and [pass](src/kalai/pass).


### Releasing versions

```sh
make deploy
```

## License

Copyright © 2020 The Kalai Authors.

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: Unicode License (https://www.unicode.org/license.html).
