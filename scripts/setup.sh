#!/bin/bash

GATEWAY_NAME=gateway-demo
FRONTEND_APP_NAME=pet-rescue-frontend
BACKEND_APP_NAME=pet-rescue-backend

build() {
  cd frontend || exit 1
  npm run build
  cd ../backend || exit 1
  ./gradlew build
  cd ..
}

deploy_all() {
  gatewayServiceInstanceIsReady() {
    [[ $(cf service $GATEWAY_NAME) =~ "create succeeded" ]]
  }

  if ! gatewayServiceInstanceIsReady; then
    cf create-service p.gateway standard $GATEWAY_NAME -c '{ "sso": { "plan": "uaa" }, "host": "gateway-demo" }'
  else
    echo "Gateway service already exists, using the existing one."
  fi

  cf push

  while ! gatewayServiceInstanceIsReady; do
    echo "Waiting for service creation to be successful..."
    sleep 1
  done

  cf bind-service $FRONTEND_APP_NAME $GATEWAY_NAME -c ./frontend/gateway-config.json
  cf bind-service $BACKEND_APP_NAME $GATEWAY_NAME -c ./backend/gateway-config.json
}

destroy_all() {
  gatewayBindingsExist() {
    [[ $(cf service $GATEWAY_NAME) =~ $FRONTEND_APP_NAME ]] || [[ $(cf service $GATEWAY_NAME) =~ BACKEND_APP_NAME ]]
  }

  cf unbind-service $FRONTEND_APP_NAME $GATEWAY_NAME
  cf unbind-service $BACKEND_APP_NAME $GATEWAY_NAME

  while gatewayBindingsExist; do
    echo "Waiting for bindings to be gone..."
    sleep 1
  done

  cf ds -f $GATEWAY_NAME
  cf d -r -f $FRONTEND_APP_NAME
  cf d -r -f $BACKEND_APP_NAME
}

case $1 in
build)
  build
  deploy_all
  ;;
deploy)
  deploy_all
  ;;
destroy)
  destroy_all
  ;;
*)
  echo 'Unknown command. Please specify "deploy" or "destroy"'
  ;;
esac
