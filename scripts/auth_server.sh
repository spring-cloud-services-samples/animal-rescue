#!/bin/bash

CONTAINER_NAME=uaa-for-animal-rescue
IMAGE_NAME=springcloudservices/uaa-server:sample

stop() {
  printf '\n======== Stopping uaa server container ========\n'
  docker container stop $CONTAINER_NAME
  printf '\n======== Removing uaa server container ========\n'
  docker container rm $CONTAINER_NAME
}

start() {
  printf '\n========Building uaa server image ========\n'
  docker image build -t springcloudservices/uaa-server:sample ./auth/
  if docker ps -a | grep -q uaa-for-animal-rescue; then
    echo "container already exists, stopping..."
    stop
  fi

  printf '\n======== Starting uaa server container ========\n'
  docker container run --publish 40000:8080 --detach --name $CONTAINER_NAME --env UAA_CONFIG_YAML='{issuer.uri: "http://localhost:40000/uaa"}' $IMAGE_NAME
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
