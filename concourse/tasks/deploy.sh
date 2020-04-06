#!/bin/bash

set -euo pipefail

readonly SRC_PATH="${SRC_PATH:?must be set}"

# Log in with CF CLI
readonly CF_ORG="${CF_ORG:?must be set}"
readonly CF_SPACE="${CF_SPACE:?must be set}"
readonly CF_API_HOST="${CF_API_HOST:?must be set}"
readonly CF_USERNAME="${CF_USERNAME:?must be set}"
readonly CF_PASSWORD="${CF_PASSWORD:?must be set}"
readonly SKIP_SSL_VALIDATION="${SKIP_SSL_VALIDATION:=false}"

echo "* Logging into ${CF_API_HOST}"
cf login -a "${CF_API_HOST}" -u "${CF_USERNAME}" -p "${CF_PASSWORD}" -o "${CF_ORG}" -s "${CF_SPACE}" "$([[ $SKIP_SSL_VALIDATION == "true" ]] && echo "--skip-ssl-validation")"

# Build and deploy
pushd "${SRC_PATH}" > /dev/null

echo "* Build"
./scripts/cf_deploy.sh init

echo "* Deploy"
./scripts/cf_deploy.sh upgrade || true
./scripts/cf_deploy.sh deploy
