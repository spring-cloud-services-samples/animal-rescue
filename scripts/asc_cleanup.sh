#!/bin/bash

set -euo pipefail

readonly BACKEND_APP="animal-rescue-backend"
readonly FRONTEND_APP="animal-rescue-frontend"

az spring-cloud gateway route-config remove --name $BACKEND_APP
az spring-cloud gateway route-config remove --name $FRONTEND_APP
az spring-cloud gateway clear
az spring-cloud app delete --name $BACKEND_APP
az spring-cloud app delete --name $FRONTEND_APP
az spring-cloud application-configuration-service git repo remove --name animal-rescue-config
az spring-cloud gateway update --assign-endpoint false
