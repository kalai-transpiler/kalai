.PHONY: all build clean
all: build

build:
	# Install dependencies
	clojure -P

	# Run tests
	clojure -X:test

	# Transpile & build examples
	cd examples && make

	# Transpile & build sql_builder
	cd sql_builder && make

## one offs

clean:
	cd examples && make clean
	cd sql_builder && make clean

deploy:
	clojure -T:build jar
	clojure -T:build deploy
