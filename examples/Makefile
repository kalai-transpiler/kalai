.PHONY: build clean setup transpile rust java
all: build

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
