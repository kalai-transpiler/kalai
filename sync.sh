#!/bin/bash
rsync --recursive --delete --exclude 'target' --exclude 'sync.sh' . /google/src/cloud/elango/elango-exp/google3/experimental/users/elango/oss/clj-icu-test
