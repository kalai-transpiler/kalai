.PHONY: all run_all build clean setup transpile rust java run_rust run_java
all: build run_all

CARGO := RUSTFLAGS=-Awarnings cargo

run_all: run_rust run_java

clean:
	rm -rf java/src java/build
	rm -rf rust/src rust/target

setup:
	clojure --version && $(CARGO) --version && gradle --version

build:
	clojure -M -m kalai.exec.main --verbose
	cd rust && ($(CARGO) fmt && $(CARGO) build) || $(CARGO) build
	cd java && (gradle googleJavaFormat && gradle build) || gradle build

transpile:
	clojure -M -m kalai.exec.main --verbose
	cd rust && $(CARGO) fmt

rust:
	cd rust && $(CARGO) build

java:
	cd java && gradle build

run_rust:
	cd rust && $(CARGO) test && $(CARGO) run sql_builder_run_examples

run_java:
	cd java && gradle -PmainClass=sqlbuilder.Examples run
