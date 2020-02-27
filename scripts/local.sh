#!/bin/bash

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
  if [[ $1 == '--quiet' ]]; then
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
    kill "$(lsof -i:8080 -t)"
  fi
}

startBackend() {
  cd backend || exit 1
  stopBackend
  printf "\n======== Starting backend ========\n"

  local gradle_opts=''

  if [[ $LOGIN_MODE == 'oauth2' ]]; then
    gradle_opts='-Plocal'
  fi

  if [[ $1 == '--quiet' ]]; then
    echo "Entering quiet mode, output goes here ./scripts/out/backend_output.log"
    ./gradlew $gradle_opts bootRun &>../scripts/out/backend_output.log &
  else
    ./gradlew $gradle_opts bootRun &
  fi
  cd ..
}

start() {
  mkdir -p ./scripts/out

  if [[ $LOGIN_MODE == 'oauth2' ]]; then
    ./scripts/auth_server.sh start
  fi

  startBackend "$1"
  startFrontend "$1"
}

stop() {
  stopBackend
  stopFrontend
  if [[ $LOGIN_MODE == 'oauth2' ]]; then
    ./scripts/auth_server.sh stop
  fi
}

testBackend() {
  cd backend || exit 1
  printf "\n======== Running backend unit tests ========\n"
  ./gradlew test
  cd ..
}

testE2e() {
  cd e2e || exit 1
  if [[ $1 == '--quiet' ]]; then
    npm test
  else
    npm run open
  fi
  cd ..
}

case $1 in
init)
  init
  ;;
backend)
  testBackend
  ;;
ci)
  testBackend
  start '--quiet'
  testE2e '--quiet'
  stop
  ;;
e2e)
  echo 'make sure you have executed the "run" command'
  testE2e $2
  ;;
start)
  start $2
  ;;
stop)
  stop
  ;;
cleanup)
  ./scripts/auth_server.sh cleanup
  ;;
*)
  echo 'Unknown command. Please specify "init", "backend", "ci", "e2e", "start", "stop", or "cleanup"'
  ;;
esac
