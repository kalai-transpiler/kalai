.PHONY: all run_all build clean setup transpile rust java run_rust run_java deploy
all: build run_all

run_all: run_rust run_java

build:
	clojure -M -m kalai.exec.main --verbose
	cd rust && (cargo fmt && cargo build) || cargo build
	cd java && (gradle googleJavaFormat && gradle build) || gradle build

run_rust:
	cd rust && cargo test && cargo run sql_builder_run_examples

run_java:
	cd java && gradle -PmainClass=sqlbuilder.Examples run

## one offs

clean:
	rm -rf java/src java/build
	rm -rf rust/src rust/target

setup:
	clojure --version && cargo --version && gradle --version

transpile:
	clojure -M -m kalai.exec.main --verbose
	cd rust && cargo fmt

rust:
	cd rust && cargo build

java:
	cd java && gradle build
