#!/bin/bash
uaac target http://localhost:8080/uaa
uaac token client get admin -s adminsecret
uaac client add bind-client --name bind-client --authorized_grant_types client_credentials,refresh_token --scope openid,uaa.user --authorities scim.read,scim.write -s bindclientsecret
uaac client add resource-client --name resource-client --authorized_grant_types client_credentials,refresh_token --scope openid --authorities scim.read -s resourceclientsecret
uaac client add app-client --name app-client --authorized_grant_types password,refresh_token --scope openid,uaa.user --authorities uaa.none -s appclientsecret
uaac token client get bind-client -s bindclientsecret
uaac user add app-user --emails appuser@pivotal.io -p appuserpassword
uaac group add config-server-users-123
uaac member add config-server-users-123 app-user

#Test permissions
uaac token client get resource-client -s resourceclientsecret
uaac user get app-user

uaac token owner get app-client app-user -s appclientsecret -p appuserpassword


# Commands to test
uaac client add test-client --name test-client --authorized_grant_types client_credentials --authorities config_server_123 -s clientsecret
uaac token client get test-client -s clientsecret
uaac context