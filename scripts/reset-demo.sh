#!/usr/bin/env bash

set -euo pipefail

readonly ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/.."

build_animal_rescue() {
  ./gradlew assemble
}

set_cf_target() {
  cf target -o springone -s demo
}

delete_old_space() {
  while cf space demo; do
    cf delete-space -f demo || sleep 5
  done
}

create_new_space() {
  cf create-space demo
  cf target -s demo
}

create_gateway() {
  cf create-service p.gateway standard gateway-demo -c gateway-config.json
}

main() {
  cd "$ROOT_DIR"
  build_animal_rescue
  set_cf_target
  delete_old_space
  create_new_space
  create_gateway
}

main
