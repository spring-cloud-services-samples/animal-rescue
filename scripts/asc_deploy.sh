#!/bin/bash

set -euo pipefail

readonly PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")"/.. && pwd)"
readonly BACKEND_ROUTES="$PROJECT_ROOT/backend/asc/api-route-config.json"

# TODO: set name via param
# az configure --defaults group=paly-animal-rescue spring-cloud=paly-animal-rescue

function configure_acs() {
    az spring-cloud application-configuration-service git repo add --name animal-rescue-config --label main --patterns "backend/default,frontend/default" --uri "https://github.com/maly7/animal-rescue-config"
}

function deploy_backend() {
    az spring-cloud app create --name backend --instance-count 1 --memory 1Gi
    az spring-cloud application-configuration-service bind --app backend

    pushd $PROJECT_ROOT/backend
    az spring-cloud app deploy --name backend --source-path backend --config-file-pattern backend

    pushd $PROJECT_ROOT
}

function read_secret_prop() {
  grep "${1}" "$PROJECT_ROOT/secrets/sso.properties" | cut -d'=' -f2
}

function configure_gateway() {
    az spring-cloud gateway update --assign-endpoint true
    local gateway_url=$(az spring-cloud gateway show | jq -r '.properties.url')

    az spring-cloud gateway update \
      --api-description "animal rescue api" \
      --api-title "animal rescue" \
      --api-version "v.01" \
      --server-url "https://$gateway_url" \
      --allowed-origins "*" \
      --client-id "$(read_secret_prop 'client-id')" \
      --client-secret "$(read_secret_prop 'client-secret')" \
      --scope "$(read_secret_prop 'scope')" \
      --issuer-uri "$(read_secret_prop 'issuer-uri')"

    az spring-cloud gateway route-config create \
      --name backend \
      --app-name backend \
      --routes-file "$BACKEND_ROUTES"
}

function main() {
    configure_acs
    deploy_backend
    configure_gateway
}

main