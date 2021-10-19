#!/bin/bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/.."
GATEWAY_NAME=gateway-demo
FRONTEND_APP_NAME=animal-rescue-frontend
BACKEND_APP_NAME=animal-rescue-backend

init() {
  ./gradlew :frontend:npm_ci
}

build() {
  ./gradlew assemble
}

gatewayDetailContains() {
  [[ $(cf service $GATEWAY_NAME) =~ $1 ]]
}

serviceSummaryContains() {
  [[ $(cf services) =~ $1 ]]
}

push() {
  cf push
}

bind_all() {
  # Bind backend app
  if gatewayDetailContains "$BACKEND_APP_NAME"; then
    unbind $BACKEND_APP_NAME
  fi

  cf bind-service $BACKEND_APP_NAME $GATEWAY_NAME -c ./backend/api-route-config.json

  # Bind frontend app
  if gatewayDetailContains "$FRONTEND_APP_NAME"; then
    unbind $FRONTEND_APP_NAME
  fi

  cf bind-service $FRONTEND_APP_NAME $GATEWAY_NAME -c ./frontend/api-route-config.json
  while gatewayDetailContains "create in progress"; do
    echo "Waiting for binding $FRONTEND_APP_NAME to finish..."
    sleep 1
  done

  cf restart $BACKEND_APP_NAME
}

unbind() {
  cf unbind-service "$1" "$GATEWAY_NAME"
  while gatewayDetailContains "$1"; do
    echo "Waiting for unbinding $1 to finish..."
    sleep 1
  done
}

unbind_all() {
  unbind $FRONTEND_APP_NAME
  unbind $BACKEND_APP_NAME
}

routes_update_for_app() {
  app_name=$1
  sub_dir=$2

  app_guid=$(cf app "$app_name" --guid)
  gateway_service_instance_id="$(cf service $GATEWAY_NAME --guid)"
  gateway_url=$(cf curl /v2/service_instances/"$gateway_service_instance_id" | jq .entity.dashboard_url | sed "s/\/scg-dashboard//" | sed "s/\"//g")

  printf "Calling dynamic binding update endpoint for %s...\n=====\n\n" "$app_name"
  status_code=$(curl -k -XPUT "$gateway_url/actuator/bound-apps/$app_guid/routes" -d "@$ROOT_DIR/$sub_dir/api-route-config.json" \
    -H "Authorization: $(cf oauth-token)" -H "Content-Type: application/json" --write-out %{http_code} -vsS)
  if [[ $status_code == '204' ]]; then
    printf "\n=====\nBound app %s route configuration update response status: %s\n\n" "$app_name" "$status_code"
  else
    printf "\033[31m\n=====\nUpdate %s configuration failed\033[0m" "$app_name" >/dev/stderr
    exit 1
  fi
}

routes_update_all() {
  routes_update_for_app $FRONTEND_APP_NAME 'frontend'
  routes_update_for_app $BACKEND_APP_NAME 'backend'
}

deploy_all() {
  gatewayServiceInstanceIsReady() {
    gatewayDetailContains "[create|update] service instance completed"
  }
  gatewayServiceInstanceFailed() {
    gatewayDetailContains "[create|update] failed"
  }

  if ! gatewayServiceInstanceIsReady && ! gatewayServiceInstanceFailed; then
    echo "Gateway service does not exist, creating..."
    cf create-service p.gateway standard $GATEWAY_NAME -c ./gateway/api-gateway-config.json
  else
    echo "Gateway service already exists, updating..."
    cf update-service $GATEWAY_NAME -c ./gateway/api-gateway-config.json
  fi

  while ! gatewayServiceInstanceIsReady; do
    if gatewayServiceInstanceFailed; then
      printf "\033[31m\n=====\nOops, something went wrong.\n%s\n \033[0m" "$(cf service $GATEWAY_NAME)">/dev/stderr
      exit 1
    fi

    echo "Waiting for service instance to be ready..."
    sleep 1
  done

  push
  bind_all
  routes_update_all
}

destroy_all() {
  unbind_all

  cf delete-service -f $GATEWAY_NAME
  cf delete -r -f $FRONTEND_APP_NAME
  cf delete -r -f $BACKEND_APP_NAME

  while serviceSummaryContains "$GATEWAY_NAME"; do
    echo "Waiting for $GATEWAY_NAME to be deleted..."
    sleep 1
  done
}

cd "$ROOT_DIR" || exit 1

case $1 in
init)
  init
  ;;
push)
  build
  push
  ;;
rebind)
  bind_all
  ;;
dynamic_route_config_update)
  routes_update_all
  ;;
deploy)
  deploy_all
  ;;
upgrade)
  cf update-service $GATEWAY_NAME -c '{"upgrade": true}'
  ;;
destroy)
  destroy_all
  ;;
*)
  echo 'Unknown command. Please specify "init", "push", "dynamic_route_config_update", "rebind", "deploy", "upgrade" or "destroy"'
  ;;
esac
