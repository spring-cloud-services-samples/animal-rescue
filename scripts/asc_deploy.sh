#!/bin/bash

set -euo pipefail

readonly PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")"/.. && pwd)"
readonly BACKEND_APP="animal-rescue-backend"
readonly FRONTEND_APP="animal-rescue-frontend"

RESOURCE_GROUP=''
SPRING_CLOUD_INSTANCE=''
JWK_SET_URI=''

function configure_defaults() {
  echo "Configure azure defaults resource group: $RESOURCE_GROUP and spring-cloud $SPRING_CLOUD_INSTANCE"
  az configure --defaults group=$RESOURCE_GROUP spring-cloud=$SPRING_CLOUD_INSTANCE
}

function create_nodejs_builder() {
  echo "Creating builder with nodejs buildpack and no bindings"
  az spring-cloud build-service builder create -n nodejs-only --builder-file "${PROJECT_ROOT}/frontend/asc/nodejs_builder.json" --no-wait
}

function configure_acs() {
  echo "Configuring Application Configuration Service to use repo: https://github.com/spring-cloud-services-samples/animal-rescue"
  az spring-cloud application-configuration-service git repo add --name animal-rescue-config --label Azure --patterns "default,backend" --uri "https://github.com/spring-cloud-services-samples/animal-rescue" --search-paths config
}

function create_backend_app() {
  echo "Creating backend application"
  az spring-cloud app create --name $BACKEND_APP --instance-count 1 --memory 1Gi
  az spring-cloud application-configuration-service bind --app $BACKEND_APP

  local backend_routes=''
  if [[ -f "$PROJECT_ROOT/secrets/sso.properties" ]]; then
    backend_routes="$PROJECT_ROOT/backend/asc/api-route-config.json"
  else
    backend_routes="$PROJECT_ROOT/backend/asc/api-route-config-no-sso.json"
  fi

  echo "Adding routes for backend application using definitions at $backend_routes"
  az spring-cloud gateway route-config create \
    --name $BACKEND_APP \
    --app-name $BACKEND_APP \
    --routes-file "$backend_routes"
}

function create_frontend_app() {
  echo "Creating frontend application"
  az spring-cloud app create --name $FRONTEND_APP --instance-count 1 --memory 1Gi

  local frontend_routes=''
  if [[ -f "$PROJECT_ROOT/secrets/sso.properties" ]]; then
    frontend_routes="$PROJECT_ROOT/frontend/asc/api-route-config.json"
  else
    frontend_routes="$PROJECT_ROOT/frontend/asc/api-route-config-no-sso.json"
  fi

  echo "Adding routes for frontend application using definitions at $frontend_routes"
  az spring-cloud gateway route-config create \
    --name $FRONTEND_APP \
    --app-name $FRONTEND_APP \
    --routes-file "$frontend_routes"
}

function deploy_backend() {
  pushd $PROJECT_ROOT/backend

  if [[ -z "$JWK_SET_URI" ]]; then
    echo "Deploying backend application without jwk_set_uri being configured, will use default value"
    az spring-cloud app deploy --name $BACKEND_APP --config-file-pattern backend --source-path .
  else
    echo "Deploying backend application, configured to use jwk_set_uri $JWK_SET_URI"
    az spring-cloud app deploy --name $BACKEND_APP --config-file-pattern backend --env "SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWKSETURI=$JWK_SET_URI" --source-path .
  fi

  pushd $PROJECT_ROOT
}

function deploy_frontend() {
  echo "Deploying frontend application"
  pushd $PROJECT_ROOT/frontend
  rm -rf node_modules/
  az spring-cloud app deploy --name $FRONTEND_APP --builder nodejs-only --source-path .

  pushd $PROJECT_ROOT
}

function read_secret_prop() {
  grep "${1}" "$PROJECT_ROOT/secrets/sso.properties" | cut -d'=' -f2
}

function configure_gateway() {
  az spring-cloud gateway update --assign-endpoint true
  local gateway_url=$(az spring-cloud gateway show | jq -r '.properties.url')

  if [[ -f "$PROJECT_ROOT/secrets/sso.properties" ]]; then
    echo "Configuring Spring Cloud Gateway with SSO enabled"
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
  else
    echo "Configuring Spring Cloud Gateway without SSO enabled"
    az spring-cloud gateway update \
      --api-description "animal rescue api" \
      --api-title "animal rescue" \
      --api-version "v.01" \
      --server-url "https://$gateway_url" \
      --allowed-origins "*"
  fi
}

function configure_portal() {
  az spring-cloud api-portal update --assign-endpoint true

  if [[ -f "$PROJECT_ROOT/secrets/sso.properties" ]]; then
    echo "Configuring API Portal with SSO properties"
    az spring-cloud api-portal update \
      --client-id "$(read_secret_prop 'client-id')" \
      --client-secret "$(read_secret_prop 'client-secret')" \
      --scope "openid,profile,email" \
      --issuer-uri "$(read_secret_prop 'issuer-uri')"
  fi
}

function print_done() {
  local gateway_url=$(az spring-cloud gateway show | jq -r '.properties.url')
  local portal_url=$(az spring-cloud api-portal show | jq -r '.properties.url')
  echo "API Portal is available at https://$portal_url"
  echo "Animal Rescue successfully deployed. The application can be accessed at https://$gateway_url"
}

function main() {
  configure_defaults
  create_nodejs_builder
  configure_acs
  configure_gateway
  configure_portal
  create_backend_app
  create_frontend_app
  deploy_backend
  deploy_frontend
  print_done
}

function usage() {
  echo 1>&2
  echo "Usage: $0 -g <resource_group> -s <spring_cloud_instance> [-u <jwk_set_uri>]" 1>&2
  echo 1>&2
  echo "Options:" 1>&2
  echo "  -g <namespace>              the Azure resource group to use for the deployment" 1>&2
  echo "  -s <spring_cloud_instance>  the name of the Azure Spring Cloud Instance to use" 1>&2
  echo "  -u <jwk_set_uri>            the application property for spring.security.oauth2.resourceserver.jwt.jwk-set-uri to be provided to the backend application" 1>&2
  echo 1>&2
  exit 1
}

function check_args() {
  if [[ -z $RESOURCE_GROUP ]]; then
    echo "Provide a valid resource group with -g"
    usage
  fi

  if [[ -z $SPRING_CLOUD_INSTANCE ]]; then
    echo "Provide a valid spring cloud instance name with -s"
    usage
  fi
}

while getopts ":g:s:u:" options; do
  case "$options" in
  g)
    RESOURCE_GROUP="$OPTARG"
    ;;
  s)
    SPRING_CLOUD_INSTANCE="$OPTARG"
    ;;
  u)
    JWK_SET_URI="$OPTARG"
    ;;
  *)
    usage
    exit 1
    ;;
  esac

  case $OPTARG in
  -*)
    echo "Option $options needs a valid argument"
    exit 1
    ;;
  esac
done

check_args
main
