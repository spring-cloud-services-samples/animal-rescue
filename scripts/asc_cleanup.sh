#!/bin/bash

set -euo pipefail

az spring-cloud gateway route-config remove --name backend
az spring-cloud gateway clear
az spring-cloud app delete --name backend
az spring-cloud application-configuration-service git repo remove --name animal-rescue-config
az spring-cloud gateway update --assign-endpoint false
