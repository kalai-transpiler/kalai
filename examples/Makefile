.PHONY: build clean setup
#all: clean build

clean:
	rm -rf java/src java/build
	rm -rf rust/src rust/target

setup:
	clj --version && cargo --version && gradle --version

build:
	clj -M -m kalai.exec.main --src-dir src/main/clj --verbose