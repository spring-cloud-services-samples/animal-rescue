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

gatewayDetailContains() {
  [[ $(cf service $GATEWAY_NAME) =~ $1 ]]
}

serviceSummaryContains() {
  [[ $(cf services) =~ $1 ]]
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

  cf push

  while ! gatewayServiceInstanceIsReady; do
    echo "Waiting for service creation to be successful..."
    sleep 1
  done

  cf bind-service $BACKEND_APP_NAME $GATEWAY_NAME -c ./backend/gateway-config.json
  cf bind-service $FRONTEND_APP_NAME $GATEWAY_NAME -c ./frontend/gateway-config.json

  while gatewayDetailContains "create in progress"; do
    echo "Waiting for bindings to finish..."
    sleep 1
  done
  cf restage $BACKEND_APP_NAME
}

destroy_all() {

  unbind() {
    appName=$1
    if gatewayDetailContains "$appName"; then
      cf unbind-service "$appName" "$GATEWAY_NAME"
    else
      echo "No need to unbind rescue services"
    fi

    while gatewayDetailContains "$appName"; do
      echo "Waiting for $appName to be unbound..."
      sleep 1
    done
  }

  unbind $FRONTEND_APP_NAME
  unbind $BACKEND_APP_NAME

  cf delete-service -f $GATEWAY_NAME
  cf delete -r -f $FRONTEND_APP_NAME
  cf delete -r -f $BACKEND_APP_NAME

  while ! serviceSummaryContains "Service instance $GATEWAY_NAME not found"; do
    echo "Waiting for $GATEWAY_NAME to be deleted..."
    sleep 1
  done
}

case $1 in
build)
  build
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
