#!/bin/sh    
set -e

# Install dependencies
lein deps

# Run tests
lein test

# Transpile & build examples
pushd examples
make
popd

# Transpile & build sql_builder
pushd sql_builder
make
popd
