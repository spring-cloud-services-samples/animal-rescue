---
page_type: sample
languages:
- java
products:
- Azure Spring Cloud
description: "Deploy Spring Boot apps using Azure Spring Cloud and Spring Cloud Gateway"
urlFragment: ""
---

# Deploy Spring Boot apps using Azure Spring Cloud and Spring Cloud Gateway

Azure Spring Cloud enables you to easily run a Spring Boot applications on Azure.

This quickstart shows you how to deploy an existing Java Spring Cloud application to Azure. When
you're finished, you can continue to manage the application via the Azure CLI or switch to using the
Azure Portal.

* [Deploy Spring Boots using Azure Spring Cloud and Spring Cloud Gateway](#deploy-spring-boot-apps-using-azure-spring-cloud-and-spring-cloud-gateway)
  * [What will you experience](#what-will-you-experience)
  * [What you will need](#what-you-will-need)
  * [Install the Azure CLI extension](#install-the-azure-cli-extension)
  * [Clone the repo](#clone-the-repo)
  * [Unit 1 - Deploy and Build Applications](#unit-1---deploy-and-build-applications)
  * [Unit 2 - Configure Single Sign On](#unit-2---configure-single-sign-on)

## What will you experience
You will:
- Provision an Azure Spring Cloud service instance.
- Configure Application Configuration Service repositories
- Deploy applications to Azure existing Spring Boot applications and build using Tanzu Build Service
- Configure routing to the applications using Spring Cloud Gateway
- Open the application
- Explore the application API with Api Portal
- Configure Single Sign On (SSO) for the application

## What you will need

In order to deploy a Java app to cloud, you need
an Azure subscription. If you do not already have an Azure
subscription, you can activate your
[MSDN subscriber benefits](https://azure.microsoft.com/pricing/member-offers/msdn-benefits-details/)
or sign up for a
[free Azure account]((https://azure.microsoft.com/free/)).

In addition, you will need the following:

| [Azure CLI version 2.17.1 or higher](https://docs.microsoft.com/cli/azure/install-azure-cli?view=azure-cli-latest)
| [Git](https://git-scm.com/)
| [`jq` utility](https://stedolan.github.io/jq/download/)
|

Note -  The [`jq` utility](https://stedolan.github.io/jq/download/). On Windows, download [this Windows port of JQ](https://github.com/stedolan/jq/releases) and add the following to the `~/.bashrc` file:
```bash
alias jq=<JQ Download location>/jq-win64.exe
```

Note - The Bash shell. While Azure CLI should behave identically on all environments, shell
semantics vary. Therefore, only bash can be used with the commands in this repo.
To complete these repo steps on Windows, use Git Bash that accompanies the Windows distribution of
Git. Use only Git Bash to complete this training on Windows. Do not use WSL.


### OR Use Azure Cloud Shell

Or, you can use the Azure Cloud Shell. Azure hosts Azure Cloud Shell, an interactive shell
environment that you can use through your browser. You can use the Bash with Cloud Shell
to work with Azure services. You can use the Cloud Shell pre-installed commands to run the
code in this README without having to install anything on your local environment. To start Azure
Cloud Shell: go to [https://shell.azure.com](https://shell.azure.com), or select the
Launch Cloud Shell button to open Cloud Shell in your browser.

To run the code in this article in Azure Cloud Shell:

1. Start Cloud Shell.

2. Select the Copy button on a code block to copy the code.

3. Paste the code into the Cloud Shell session by selecting Ctrl+Shift+V on Windows and Linux or by selecting Cmd+Shift+V on macOS.

4. Select Enter to run the code.


## Install the Azure CLI extension

Install the Azure Spring Cloud extension for the Azure CLI using the following command

```bash
    az extension add --name spring-cloud
```
Note - `spring-cloud` CLI extension `3.0.0` or later is a pre-requisite to enable the
latest Enterprise tier functionality to configure VMware Tanzu Components 

```bash
    az extension remove --name spring-cloud
    az extension add --name spring-cloud
```

## Clone the repo

### Create a new folder and clone the sample app repository to your Azure Cloud account

```bash
    mkdir source-code
    git clone https://github.com/spring-cloud-services-samples/animal-rescue
    cd animal-rescue
```

## Unit-1 - Deploy and build Applications

### Prepare your environment for deployments

Create a bash script with environment variables by making a copy of the supplied template:

```bash
    cp .scripts/setup-env-variables-azure-template.sh .scripts/setup-env-variables-azure.sh
```

Open `.scripts/setup-env-variables-azure.sh` and enter the following information:

```bash
    export SUBSCRIPTION=subscription-id # customize this
    export RESOURCE_GROUP=resource-group-name # customize this
    export SPRING_CLOUD_SERVICE=azure-spring-cloud-name # customize this
    export REGION=region-name # customize this
```

Then, set the environment:
```bash
    source .scripts/setup-env-variables-azure.sh
```

### Login to Azure
Login to the Azure CLI and choose your active subscription. Be sure to choose the active subscription that is whitelisted for Azure Spring Cloud

```bash
    az login
    az account list -o table
    az account set --subscription ${SUBSCRIPTION}
```

### Create Azure Spring Cloud service instance
Prepare a name for your Azure Spring Cloud service.  The name must be between 4 and 32 characters long and can contain only lowercase letters, numbers, and hyphens.  The first character of the service name must be a letter and the last character must be either a letter or a number.

Create a resource group to contain your Azure Spring Cloud service.

```bash
    az group create --name ${RESOURCE_GROUP} \
        --location ${REGION}
```

Create an instance of Azure Spring Cloud Enterprise.

```bash
    az spring-cloud create --name ${SPRING_CLOUD_SERVICE} \
            --sku enterprise \
            --sampling-rate 100 \
            --resource-group ${RESOURCE_GROUP} \
            --location ${REGION}
```

The service instance will take around five minutes to deploy.

Set your default resource group name and cluster name using the following commands:

```bash
    az configure --defaults \
        group=${RESOURCE_GROUP} \
        location=${REGION} \
        spring-cloud=${SPRING_CLOUD_SERVICE}
```

> Note: wait for the instance of Azure Spring Cloud to be ready before continuing

### Configure Application Configuration Service

Create a configuration repository for Application Configuration Service using the Azure CLI:

```bash
    az spring-cloud application-configuration-service git repo add --name animal-rescue-config \
        --label main \
        --patterns "default,backend" \
        --uri "https://github.com/maly7/animal-rescue-config"
```

### Configure Tanzu Build Service

Create a builder in Tanzu Build Service for the frontend application using the Azure CLI:

```bash
    az spring-cloud build-service builder create -n nodejs-only \
        --builder-file frontend/asc/nodejs_builder.json \
        --no-wait
```

### Create applications in Azure Spring Cloud

Create an application for the frontend and another for the backend:

```bash
    az spring-cloud app create --name $BACKEND_APP --instance-count 1 --memory 1Gi
    az spring-cloud app create --name $FRONTEND_APP --instance-count 1 --memory 1Gi
```

### Bind to Application Configuration Service

Bind the backend application to Application Configuration Service:

```bash
    az spring-cloud application-configuration-service bind --app $BACKEND_APP
```

### Configure Spring Cloud Gateway

Assign an endpoint and update the Spring Cloud Gateway configuration with API 
information:

```bash
    az spring-cloud gateway update --assign-endpoint true
    export GATEWAY_URL=$(az spring-cloud gateway show | jq -r '.properties.url')
    
    az spring-cloud gateway update \
      --api-description "Animal Rescue API" \
      --api-title "Animal Rescue" \
      --api-version "v.01" \
      --server-url "https://$gateway_url" \
      --allowed-origins "*"
```

Create routing rules for the backend and frontend applications:

```bash
    az spring-cloud gateway route-config create \
        --name $BACKEND_APP \
        --app-name $BACKEND_APP \
        --routes-file backend/asc/api-route-config-no-sso.json

    az spring-cloud gateway route-config create \
        --name $FRONTEND_APP \
        --app-name $FRONTEND_APP \
        --routes-file frontend/asc/api-route-config-no-sso.json
```

### Build and Deploy Applications

Deploy and build the backend application, specifying its configuration file pattern for
Application Configuration Service:

```bash
    az spring-cloud app deploy --name $BACKEND_APP \
          --config-file-pattern backend \
          --source-path backend/
```

Deploy and build the frontend application using the builder created earlier:

```bash
    az spring-cloud app deploy --name $FRONTEND_APP \
        --builder nodejs-only \
        --source-path frontend/
```

### Access the Application through Spring Cloud Gateway

Retrieve the URL for Spring Cloud Gateway and open it in a browser:

```bash
    open "https://$GATEWAY_URL"
```

You should see the Animal Rescue Application:

![](./media/animal-rescue.png)

### Explore the API using API Portal

Assign an endpoint to API Portal and open it in a browser:

```bash
    az spring-cloud api-portal update --assign-endpoint true
    export PORTAL_URL=$(az spring-cloud api-portal show | jq -r '.properties.url')
    
    open "https://$PORTAL_URL"
```

![](./media/api-portal.png)

## Unit 2 - Configure Single Sign On

In this section, you will configure SSO for Spring Cloud Gateway.

### Cleanup Previous Resources

Before getting started, cleanup resources from the previous section:

```bash
    az spring-cloud gateway route-config remove --name $BACKEND_APP
    az spring-cloud gateway route-config remove --name $FRONTEND_APP
    
    az spring-cloud gateway clear
```

### Prepare your environment for deployments

Create a bash script with environment variables by making a copy of the supplied template:

```bash
    cp .scripts/setup-sso-variables-azure-template.sh .scripts/setup-sso-variables-azure.sh
```

Open `.scripts/setup-env-variables-azure.sh` and enter the following information:

```bash
    export CLIENT_ID={your_client_id}         # customize this
    export CLIENT_SECRET={your_client_secret} # customize this
    export ISSUER_URI={your_issuer_uri}       # customize this
    export JWK_SET_URI={your_jwk_set_uri}     # customize this
```

> Note: `JWK_SET_URI` should look something like: `https://your-provider.com/discovery/keys`

Then, set the environment:
```bash
    source .scripts/setup-sso-variables-azure.sh
```

### Configure Spring Cloud Gateway

Configure Spring Cloud Gateway with SSO enabled:

```bash
    az spring-cloud gateway update --assign-endpoint true
    export GATEWAY_URL=$(az spring-cloud gateway show | jq -r '.properties.url')

    az spring-cloud gateway update \
          --api-description "Animal Rescue API" \
          --api-title "Animal Rescue" \
          --api-version "v.01" \
          --server-url "https://$GATEWAY_URL" \
          --allowed-origins "*" \
          --client-id $CLIENT_ID \
          --client-secret $CLIENT_SECRET \
          --scope $SCOPE \
          --issuer-uri $ISSUER_URI
```

Create routing rules for the backend and frontend applications:

```bash
    az spring-cloud gateway route-config create \
        --name $BACKEND_APP \
        --app-name $BACKEND_APP \
        --routes-file backend/asc/api-route-config.json

    az spring-cloud gateway route-config create \
        --name $FRONTEND_APP \
        --app-name $FRONTEND_APP \
        --routes-file frontend/asc/api-route-config.json
```

### Configure SSO Provider

Add the redirect URL output by the following script to your SSO provider:

```bash
   echo "https://$GATEWAY_URL/login/oauth2/code/sso"
```


### Deploy Spring Boot app with SSO

Deploy the backend application again, this time providing the environment variable:

```bash
    az spring-cloud app deploy --name $BACKEND_APP \
        --config-file-pattern backend \
        --env "SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_JWKSETURI=$JWK_SET_URI" \
        --source-path backend/
```

### Access the Application through Spring Cloud Gateway

Retrieve the URL for Spring Cloud Gateway and open it in a browser:

```bash
    open "https://$GATEWAY_URL"
```

You should see the Animal Rescue Application, and be able to log in using the
configured SSO provider. 

### Explore the API using API Portal

Open API Portal in a browser, use the "Authorize" button to authenticate with the API:

```bash
    open "https://$PORTAL_URL"
```

## Next Steps

In this quickstart, you've deployed a Spring Boot application and a nodejs application using Azure CLI.
You also configured VMware Tanzu components in the enterprise tier. To learn more about 
Azure Spring Cloud, go to:

- [Azure Spring Cloud](https://azure.microsoft.com/en-us/services/spring-cloud/)
- [Azure Spring Cloud docs](https://docs.microsoft.com/en-us/azure/spring-cloud/quickstart-provision-service-instance-enterprise?tabs=azure-portal)
- [Deploy Spring microservices from scratch](https://github.com/microsoft/azure-spring-cloud-training)
- [Deploy existing Spring microservices](https://github.com/Azure-Samples/azure-spring-cloud)
- [Azure for Java Cloud Developers](https://docs.microsoft.com/en-us/azure/java/)
- [Spring Cloud Azure](https://cloud.spring.io/spring-cloud-azure/)
- [Spring Cloud](https://spring.io/projects/spring-cloud)
- [Application Configuration Service]()
- [Spring Cloud Gateway]()
- [API Portal]()
