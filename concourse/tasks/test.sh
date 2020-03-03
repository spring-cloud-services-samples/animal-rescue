#!/bin/bash

set -euo pipefail

readonly SRC_PATH="${SRC_PATH:?must be set}"

pushd "${SRC_PATH}" > /dev/null

./scripts/local.sh init
./scripts/local.sh ci
