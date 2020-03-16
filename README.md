# Kalai Transpiler

Kalai transpiler is a source-to-source transpiler to convert Clojure to multiple target languages (Rust, C++, Java, ...).

The goal of Kalai is to allow useful algorithms to be encoded once and then automatically be made available natively to other target programming languages.

## Usage

### Implemented target languages

Implemented target languages are defined as namespaced keywords in `kalai.emit.langs`.  Currently, they are:

- Rust
- C++
- Java

Clojure supports namespaced keywords to enable the dynamic dispatch fallback hierarchies for multimethods.  The namespaced keywords for the target languages follow the Clojure derivation tree:

- `::l/curlybrace` ("curly brace" languages)
  * `::l/rust` (Rust)
  * `::l/cpp` (C++)
  * `::l/java` (Java)

### Supported forms

The forms that are currently supported are listed in [`interface.clj`](./src/kalai/emit/interface.clj).

### Example

If you have code written in `your.namespace`, then you can emit code as follows:

```clj
(require '[kalai.common :refer :all])
(require '[kalai.emit.util :as emit-util])
(require '[kalai.emit.langs :as l])
(require '[clojure.tools.analyzer.jvm :as az])

(def ast-seq (az/analyze-ns 'your.namespace))
(def java-strs (emit-util/emit-analyzed-ns-asts ast-seq ::l/rust))
(run! println java-strs)
```

Example demo 1 has input code at [`src/kalai/demo/demo01.clj`](./src/kalai/demo/demo01.clj) and emitter tests at [`test/kalai/demo/demo01.clj`](./test/kalai/demo/demo01_test.clj).

Example demo 2 has input code at [`src/kalai/demo/demo02.clj`](./src/kalai/demo/demo02.clj) and emitter tests at [`test/kalai/demo/demo02.clj`](./test/kalai/demo/demo02_test.clj) and logic tests at [`test/kalai/demo/demo02_logic_test.clj`](./test/kalai/demo/demo02_logic_test.clj).

## License

Copyright © 2020 The Kalai Authors.

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: Unicode License (https://www.unicode.org/license.html).
