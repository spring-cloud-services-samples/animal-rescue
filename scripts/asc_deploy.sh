#!/bin/bash

set -euo pipefail

readonly PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")"/.. && pwd)"
readonly BACKEND_ROUTES="$PROJECT_ROOT/backend/asc/api-route-config.json"
readonly FRONTEND_ROUTES="$PROJECT_ROOT/frontend/asc/api-route-config.json"
readonly BACKEND_APP="animal-rescue-backend"
readonly FRONTEND_APP="animal-rescue-frontend"

# TODO: set name via param
# az configure --defaults group=paly-animal-rescue spring-cloud=paly-animal-rescue

# TODO:
# create private config repo to load sso secret

function configure_acs() {
  az spring-cloud application-configuration-service git repo add --name animal-rescue-config --label main --patterns "backend/default,frontend/default" --uri "https://github.com/maly7/animal-rescue-config"
}

function deploy_backend() {
  az spring-cloud app create --name $BACKEND_APP --instance-count 1 --memory 1Gi
  az spring-cloud application-configuration-service bind --app $BACKEND_APP

  pushd $PROJECT_ROOT/backend
  az spring-cloud app deploy --name $BACKEND_APP --config-file-pattern backend

  pushd $PROJECT_ROOT
}

function deploy_frontend() {
#  az spring-cloud app create --name $FRONTEND_APP --instance-count 1 --memory 1Gi

  pushd $PROJECT_ROOT/frontend
  az spring-cloud app deploy --name $FRONTEND_APP --builder nodejs-only

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
    --client-id "$(read_secret_prop 'client-id')" \
    --client-secret "$(read_secret_prop 'client-secret')" \
    --scope "$(read_secret_prop 'scope')" \
    --issuer-uri "$(read_secret_prop 'issuer-uri')"
}

function configure_backend_routes() {
  az spring-cloud gateway route-config create \
    --name $BACKEND_APP \
    --app-name $BACKEND_APP \
    --routes-file "$BACKEND_ROUTES"
}

function configure_frontend_routes() {
  az spring-cloud gateway route-config create \
    --name $FRONTEND_APP \
    --app-name $FRONTEND_APP \
    --routes-file "$FRONTEND_ROUTES"
}

function main() {
  #  configure_acs
  #  deploy_backend
      configure_gateway
  #  configure_backend_routes
#  deploy_frontend
  #  configure_frontend_routes
}

main
