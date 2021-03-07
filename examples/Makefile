.PHONY: build clean setup
all: build

clean:
	rm -rf java/src java/build
	rm -rf rust/src rust/target

setup:
	clojure --version && cargo --version && gradle --version

build:
	clojure -M -m kalai.exec.main --src-dir src/main/clj --verbose
	cd rust && cargo build
	cd java && gradle build

