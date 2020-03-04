#!/bin/bash

set -euo pipefail

QUIET_MODE="--quiet"

init() {
  cd frontend || exit 1
  npm ci
  cd ../e2e || exit 1
  npm ci
}

stopFrontend() {
  if lsof -i:3000 -t &> /dev/null; then
    printf "\n======== Stopping frontend ========\n"
    pkill node
  fi
}

startFrontend() {
  cd frontend || exit 1
  stopFrontend

  printf "\n======== Starting frontend ========\n"
  if [[ $1 == "$QUIET_MODE" ]]; then
    echo "Entering quiet mode, output goes here ./scripts/out/frontend_output.log"
    BROWSER=none npm start &>../scripts/out/frontend_output.log &
  else
    npm start &
  fi
  cd ..
}

stopBackend() {
  if lsof -i:8080 -t &> /dev/null; then
    printf "\n======== Stopping backend ========\n"
    pkill java
  fi
}

startBackend() {
  cd backend || exit 1
  stopBackend
  printf "\n======== Starting backend ========\n"

  if [[ $1 == "$QUIET_MODE" ]]; then
    echo "Entering quiet mode, output goes here ./scripts/out/backend_output.log"
    ./gradlew bootRun &>../scripts/out/backend_output.log &
  else
    ./gradlew bootRun &
  fi
  cd ..
}

start() {
  mkdir -p ./scripts/out

  startBackend "$1"
  startFrontend "$1"
}

stop() {
  stopBackend
  stopFrontend
}

testBackend() {
  cd backend || exit 1
  printf "\n======== Running backend unit tests ========\n"
  ./gradlew test
  cd ..
}

testE2e() {
  cd e2e || exit 1
  if [[ $1 == "$QUIET_MODE" ]]; then
    npm test
  else
    npm run open
  fi
  cd ..
}

trap stop SIGINT

case $1 in
init)
  init
  ;;
backend)
  testBackend
  ;;
ci)
  testBackend
  start $QUIET_MODE
  testE2e $QUIET_MODE
  stop
  ;;
e2e)
  echo 'make sure you have executed the "run" command'
  testE2e "${2:-}"
  ;;
start)
  start "${2:-}"
  if [[ ${2:-} != "$QUIET_MODE" ]]; then
    wait
  fi
  ;;
stop)
  stop
  ;;
*)
  echo 'Unknown command. Please specify "init", "backend", "ci", "e2e", "start( --quiet)", or "stop"'
  ;;
esac
