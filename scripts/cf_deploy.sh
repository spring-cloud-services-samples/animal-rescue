#!/bin/bash

set -euo pipefail

ROOT_DIR=$(pwd)
GATEWAY_NAME=gateway-demo
FRONTEND_APP_NAME=animal-rescue-frontend
BACKEND_APP_NAME=animal-rescue-backend

init() {
  cd "$ROOT_DIR/frontend" || exit 1
  npm ci
}

build() {
  cd "$ROOT_DIR/frontend" || exit 1
  npm run build
  cd "$ROOT_DIR/backend" || exit 1
  ./gradlew clean bootJar
}

gatewayDetailContains() {
  [[ $(cf service $GATEWAY_NAME) =~ $1 ]]
}

serviceSummaryContains() {
  [[ $(cf services) =~ $1 ]]
}

push() {
  cd "$ROOT_DIR" || exit 1
  cf push || true # The backend app will fail to start due to lack of gateway binding
}

bind_all() {
  cd "$ROOT_DIR" || exit 1

  # Bind backend app
  if gatewayDetailContains "$BACKEND_APP_NAME"; then
    unbind $BACKEND_APP_NAME
  fi

  cf bind-service $BACKEND_APP_NAME $GATEWAY_NAME -c ./backend/gateway-config.json

  while gatewayDetailContains "create in progress"; do
    echo "Waiting for binding $BACKEND_APP_NAME to finish..."
    sleep 1
  done

  # Bind frontend app
  if gatewayDetailContains "$FRONTEND_APP_NAME"; then
    unbind $FRONTEND_APP_NAME
  fi

  cf bind-service $FRONTEND_APP_NAME $GATEWAY_NAME -c ./frontend/gateway-config.json
  while gatewayDetailContains "create in progress"; do
    echo "Waiting for binding $FRONTEND_APP_NAME to finish..."
    sleep 1
  done

  # Restage backend app
  cf restage $BACKEND_APP_NAME
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

deploy_all() {
  cd "$ROOT_DIR" || exit 1

  gatewayServiceInstanceIsReady() {
    gatewayDetailContains "[create|udpate] service instance completed"
  }

  if ! gatewayServiceInstanceIsReady; then
    echo "Gateway service does not exist, creating..."
    cf create-service p.gateway standard $GATEWAY_NAME -c ./gateway-config.json
  else
    echo "Gateway service already exists, updating..."
    cf update-service $GATEWAY_NAME -c ./gateway-config.json
  fi

  push

  while ! gatewayServiceInstanceIsReady; do
    echo "Waiting for service instance to be ready..."
    sleep 1
  done

  bind_all
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

case $1 in
init)
  init
  build
  ;;
push)
  build
  push
  ;;
rebind)
  bind_all
  ;;
deploy)
  deploy_all
  ;;
destroy)
  destroy_all
  ;;
*)
  echo 'Unknown command. Please specify "init", "push", "rebind", "deploy" or "destroy"'
  ;;
esac
