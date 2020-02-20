#!/bin/bash

CONTAINER_NAME=uaa-for-animal-rescue
IMAGE_NAME=springcloudservices/uaa-server:sample

stop() {
  docker container stop $CONTAINER_NAME
  docker container rm $CONTAINER_NAME
}

start() {
  echo 'Building uaa server image...'
  docker image build -t springcloudservices/uaa-server:sample ./auth/
  if docker ps -a | grep -q uaa-for-animal-rescue; then
    echo "container already exists, stopping..."
    stop
  fi

  echo 'Starting uaa server container...'
  docker container run --publish 40000:8080 --name $CONTAINER_NAME --env UAA_CONFIG_YAML='{issuer.uri: "http://localhost:40000/uaa"}' $IMAGE_NAME
}

case $1 in
start)
  start
  ;;
stop)
  stop
  ;;
cleanup)
  docker image rm $IMAGE_NAME
  ;;
*)
  echo 'Unknown command. Please specify "start", "stop", or "cleanup"'
  ;;
esac
