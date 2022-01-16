.PHONY: all run_all build clean setup transpile rust java run_rust run_java
all: build run_all

run_all: run_rust run_java

clean:
	rm -rf java/src java/build
	rm -rf rust/src rust/target

setup:
	clojure --version && cargo --version && gradle --version

build:
	clojure -M -m kalai.exec.main --verbose
	cd rust && (cargo fmt && cargo build) || cargo build
	cd java && (gradle googleJavaFormat && gradle build) || gradle build

transpile:
	clojure -M -m kalai.exec.main --verbose
	cd rust && cargo fmt

rust:
	cd rust && cargo build

java:
	cd java && gradle build

run_rust:
	cd rust && cargo run run_examples

run_java:
	java -cp java/build/classes/java/main a.Demo01
	java -cp java/build/classes/java/main a.Demo02
	java -cp java/build/classes/java/main a.Demo03
	java -cp java/build/classes/java/main b.Loops
	java -cp java/build/classes/java/main b.Requirer
	java -cp java/build/classes/java/main b.Simple
	java -cp java/build/classes/java/main b.TypeAlias
	java -cp java/build/classes/java/main b.Variable
