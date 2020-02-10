#!/bin/bash

GATEWAY_NAME=gateway-demo
FRONTEND_APP_NAME=pet-rescue-frontend
BACKEND_APP_NAME=pet-rescue-backend

deploy_all() {
  gatewayServiceInstanceIsReady() {
    [[ $(cf service $GATEWAY_NAME) =~ "create succeeded" ]]
  }

  if ! gatewayServiceInstanceIsReady; then
    cf create-service p.gateway standard $GATEWAY_NAME -c '{ "sso": { "plan": "uaa" }, "host": "gateway-demo" }'
  else
    echo "Gateway service already exists, using the existing one."
  fi

  while ! gatewayServiceInstanceIsReady; do
    echo "Waiting for service creation to be successful..."
    sleep 1
  done

  cf bind-service $FRONTEND_APP_NAME $GATEWAY_NAME -c ./frontend/gateway-config.json
  cf bind-service $BACKEND_APP_NAME $GATEWAY_NAME -c ./backend/gateway-config.json
}

destroy_all() {
  cf unbind-service $FRONTEND_APP_NAME $GATEWAY_NAME
  cf unbind-service $BACKEND_APP_NAME $GATEWAY_NAME
  cf ds -f $GATEWAY_NAME
}

case $1 in
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
