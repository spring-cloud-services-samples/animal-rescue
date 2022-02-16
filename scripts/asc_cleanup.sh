#!/bin/bash

set -euo pipefail

readonly BACKEND_APP="animal-rescue-backend"
readonly FRONTEND_APP="animal-rescue-frontend"

az spring-cloud gateway route-config remove --name $BACKEND_APP || true
az spring-cloud gateway route-config remove --name $FRONTEND_APP || true
az spring-cloud gateway clear || true
az spring-cloud api-portal clear || true
az spring-cloud app delete --name $BACKEND_APP || true
az spring-cloud app delete --name $FRONTEND_APP || true
az spring-cloud application-configuration-service git repo remove --name animal-rescue-config || true
az spring-cloud gateway update --assign-endpoint false || true
az spring-cloud build-service builder delete -n nodejs-only -y || true
