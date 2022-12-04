# Contributing

Pull requests welcome!

## Editor config

Currently, Cursive for IntelliJ is the editor we use to keep code formatting consistent.
(TBD: make this editor-agnostic.)

Editor > Code Style > Clojure > General
* Align reader conditionals - turned on
* Align map values - turned on
* Everything else - turned off
* Comment alignment column: -1
* Docstring fill width: 80

Editor > Code Style > Clojure > Tabs and Indents
* Use tab character: turned off
* Tab size: 2
* Tab indent: 2

Editor > Code Style > Clojure > Forms and Parameters
The default indentation/spacing values for form names (ex: fns and macros) are stored.
Pre-existing defaults are all fine.
For Meander macros, we need to define indentations manually for each of them.
They can be configured this way:
* Right click on a usage in code (ex: `m/rewrite`)
* In the context menu, choose "Show Context Actions"
* "Configure identation for meander.epsilon/rewrite"
* Choose "Indent"

## Local system config

* Build systems used at the command line:
  - Make (simple commands to run transpile+compile examples)
  - Gradle (compile Java transpiled code in examples)
  - Rustup (install Rust compiler, Cargo, etc.)
    * MacOS: `brew uninstall rust` `brew install rustup` `rustup-init` and start a new terminal session
    * version 1.65.0 or later
  - Java JDK (OpenJDK seems fine, no version constraint known yet)
  - For compiling/running Java via gradle, set `JAVA_HOME` environment variable to your default Java version path ([how to find Java version on macOS](https://stackoverflow.com/questions/36766028/see-all-the-java-versions-installed-on-mac)) 
  - `clojure` command-line tool (from the [official Clojure distribution](https://clojure.org/guides/getting_started),
    ex: `brew install clojure/tools/clojure` on macOS)
  - [Leiningen](https://leiningen.org/) for running unit tests (ex: locally and in CI) 
 
## Building and Executing

At the top level, run `make`. This compiles and runs tests for the Kalai implementation code, and then it
does transpilation for the `examples` dir and `sql_builder` dir.

## Editing tips

