#!/usr/bin/env bash

set -xe

# delete these later, we can preserve them from build stage
python scripts/testMMTk.py -g RBaseBaseNoGC --build-only
python scripts/testMMTk.py -g BaseBaseNoGC --build-only

# get deployment key, and checkout running script from repo
rm -rf .ci/running/build
mkdir -p .ci/running/build
cd .ci/running/build
ln -s ../../../dist/* .
cd ../../..
cp .ci/RunConfig.pm .ci/running/bin/RunConfig.pm

# arguments do not matter here?
.ci/running/bin/runbms 16 16