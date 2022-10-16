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
	cd rust && cargo test && cargo run run_examples

run_java:
	cd java && gradle -PmainClass=a.Demo01 run
	cd java && gradle -PmainClass=a.Demo02 run
	cd java && gradle -PmainClass=a.Demo03 run
	cd java && gradle -PmainClass=a.Demo04 run
	cd java && gradle -PmainClass=b.HigherOrderFunction run
	cd java && gradle -PmainClass=b.Loops run
	cd java && gradle -PmainClass=b.Requirer run
	cd java && gradle -PmainClass=b.Simple run
	cd java && gradle -PmainClass=b.TypeAlias run
	cd java && gradle -PmainClass=b.Variable run
	cd java && gradle -PmainClass=c.Persistent run
	cd java && gradle -PmainClass=c.ConjAsFunction run
