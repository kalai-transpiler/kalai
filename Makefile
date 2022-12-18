.PHONY: all build clean
all: build

clean:
	cd examples && make clean
	cd sql_builder && make clean

build:
	# Install dependencies
	clojure -P

	# Run tests
	clojure -X:test

	# Transpile & build examples
	cd examples && make

	# Transpile & build sql_builder
	cd sql_builder && make