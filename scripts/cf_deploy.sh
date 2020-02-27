#!/bin/bash

GATEWAY_NAME=gateway-demo
FRONTEND_APP_NAME=animal-rescue-frontend
BACKEND_APP_NAME=animal-rescue-backend

init() {
  cd frontend || exit 1
  npm ci
  cd ..
}

build() {
  cd frontend || exit 1
  npm build
  cd ../backend || exit 1
  ./gradlew clean bootJar
  cd ..
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

bind() {
  cf bind-service $BACKEND_APP_NAME $GATEWAY_NAME -c ./backend/gateway-config.json

  while gatewayDetailContains "create in progress"; do
    echo "Waiting for binding $BACKEND_APP_NAME to finish..."
    sleep 1
  done

  cf bind-service $FRONTEND_APP_NAME $GATEWAY_NAME -c ./frontend/gateway-config.json
  while gatewayDetailContains "create in progress"; do
    echo "Waiting for binding $FRONTEND_APP_NAME to finish..."
    sleep 1
  done

  cf restage $BACKEND_APP_NAME
}

unbind() {
  cf unbind-service "$FRONTEND_APP_NAME" "$GATEWAY_NAME"
  while gatewayDetailContains "$FRONTEND_APP_NAME"; do
    echo "Waiting for unbinding $FRONTEND_APP_NAME to finish..."
    sleep 1
  done

  cf unbind-service "$BACKEND_APP_NAME" "$GATEWAY_NAME"
  while gatewayDetailContains "$BACKEND_APP_NAME"; do
    echo "Waiting for unbinding $BACKEND_APP_NAME to finish..."
    sleep 1
  done
}

deploy_all() {
  gatewayServiceInstanceIsReady() {
    gatewayDetailContains "create service instance completed"
  }

  if ! gatewayServiceInstanceIsReady; then
    cf create-service p.gateway standard $GATEWAY_NAME -c '{ "sso": { "plan": "uaa" }, "host": "gateway-demo" }'
  else
    echo "Gateway service already exists, using the existing one."
  fi

  push

  while ! gatewayServiceInstanceIsReady; do
    echo "Waiting for service creation to be successful..."
    sleep 1
  done

  bind
}

destroy_all() {
  unbind

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
  install
  build
  ;;
push)
  build
  push
  ;;
rebind)
  unbind
  bind
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
