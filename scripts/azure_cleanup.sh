#!/bin/bash

set -euo pipefail

readonly BACKEND_APP="animal-rescue-backend"
readonly FRONTEND_APP="animal-rescue-frontend"

az spring gateway route-config remove --name $BACKEND_APP || true
az spring gateway route-config remove --name $FRONTEND_APP || true
az spring gateway clear || true
az spring api-portal clear || true
az spring app delete --name $BACKEND_APP || true
az spring app delete --name $FRONTEND_APP || true
az spring application-configuration-service git repo remove --name animal-rescue-config || true
az spring gateway update --assign-endpoint false || true
az spring build-service builder delete -n nodejs-only -y || true
