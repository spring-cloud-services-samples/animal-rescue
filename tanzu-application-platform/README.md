
# Deploy to Tanzu Application Platform (TAP)

Animal Rescue supports configuration to build and deploy to Tanzu Application Platform (TAP).  The following instructions describe an end-to-end 
process for configuring, building, and deploying the Animal Rescue application to a TAP `full` profile cluster.  Using additional configuration, the process can
be split across multiple cluster profiles (`build, view, and run`).

## Prerequisites

These instructions assume that you have a TAP 1.5.x or greater `full` profile cluster up and running with the following packages installed and [kubectl](https://kubernetes.io
docs/tasks/tools/) and the Tanzu CLI installed and configured to access your TAP cluster:

* [ytt](https://carvel.dev/ytt/)
* Tanzu TAP GUI
* Tanzu Build Services
* Tanzu Cloud Native Runtimes
* Tanzu Out of the Box Supply Chains
* Tanzu Out of the Box Templates
* Tanzu Source Controller
* Tanzu AppSSO
* Certificate Manager
* [Spring Cloud Gateway For Kubernetes](https://docs.vmware.com/en/VMware-Tanzu-Application-Platform/1.6/tap/spring-cloud-gateway-install-spring-cloud-gateway.html)

## Installation/Deployment Process

You should first determine the full URL where you intend to deploy the application.  Optimally, you should have a domain name registered with a 
DNS registrar and create an appropriate DNS entry for the hostname of the application.  This documentation assumes you will use the hostname `animal-rescue.  The full URL 
in this case would be:

```
animal-rescue.<your domain name>
```

Make a note of this URL as you will need it through various steps of the following doc.

It is assumed that you have cloned the Animal Rescue Git repository to your workstation.  All of the `ytt` commands below should be run from the
`tanzu-application-platform` directory in the cloned repository.

### Testing Pipeline Deployment

If you have a supply chain configured that includes testing, you will need to install the testing pipeline used when a build is performed.  Run the following command 
to install the pipeline replacing the following placeholder:

- **<workloadNamespace>** – Namespace where the application will be deployed

```
kubectl apply -f testingPipeline.yml -n <workloadNamespace>
```

For example:

```
kubectl apply -f testingPipeline.yml -n workloads
```

### Let's Encrypt CA Issuer

Animal Rescue enables TLS connections for security and reliable transport and utilizes the Let's Encrypt production CA. Install the Let's Encrypt
CA issuer into your cluster by running the following command and replacing the following place holder:

- **<email>** – The email address of the subject responsible for the issued certificates.
 
```
ytt -f caIssuer.yml -v email=<email> | kubectl apply -f-
```

For example:

```
ytt -f caIssuer.yml -v email=joe@joesgarage.com | kubectl apply -f-
```

### AppSSO Deployment

Animal Rescue requires the use of an AppSSO authorization server and client registration resource. 

Deploy the authorization server instance by running the following commands and replacing these place holders:

- **<serviceNamespace>** – Namespace where service instances will be deployed
- **<devDefaultAccountUsername>** – Username for authentication
- **<devDefaultAccountPassword>** – Password for authentication

```
ytt -f appSSOInstance.yml -v serviceNamespace=<serviceNamespace> -v devDefaultAccountUsername=<devDefaultAccountUsername> -v devDefaultAccountPassword=<devDefaultAccountPassword> | kubectl apply -f-
```

For example:

```
ytt -f appSSOInstance.yml -v serviceNamespace=service-instances -v devDefaultAccountUsername=animal -v devDefaultAccountPassword=rescue | kubectl apply -f-
```

Next, create a ClassClaim resource by running the following command and replacing the `<workloadNamespace>` placeholder with the namespace where the application will be deployed. 
This will also created the secret that will contain the AppSSO credential information that will used by the Spring Cloud Gateway.

- **<workloadNamespace>** – Namespace where the application will be deployed

```
ytt -f workloadRegistrationResource.yml -v workloadNamespace=<workloadNamespace>| kubectl apply -f-
```

For example:

```
ytt -f workloadRegistrationResource.yml -v workloadNamespace=workloads | kubectl apply -f-
```

Next, obtain the appSSO issuerURI by running the following command replacing <serviceNamespace> with the name of the namespace where the service instance 
was deployed:

```
kubectl get authserver -n <serviceNamespace>
```

Save the Issuer URI as you may need to create a new entry in your DNS registrar for this URL; this will likely be
necessary if you are NOT using wildcard records in your DNS registrar.  DNS is also required in order for the TLS certificate to be issued for the auth server.

Finally, obtain the appSSO credential secret name by running the following command replacing` <workloadNamespace>` placeholder with the namespace where the application will be deployed.  Save the `status.claimed-resource.name` field value as this will be the secret name that will need to provided when configuring the Spring Cloud Gateway.

```
tanzu service class-claim get appsso-animal-rescue -n <workloadNamespace>
```

### Workload Build And Deployment

To build the application services, execute the following command to apply the workload resources to your cluster while replacing these placeholders: Modify the
`<workloadNamespace>` placeholder with the namespace where the application will be deployed.

- **<workloadNamespace>** – Namespace where the application will be deployed

```
ytt -f workloads.yml -v workloadNamespace=<workloadNamespace> | kubectl apply -f-
```

For example:

```
ytt -f workloads.yml -v workloadNamespace=workloads | kubectl apply -f-
```

**NOTE** If the front end service fails to build (due to a known issue at the time of writing), you can run the following command from the root directory of the cloned
repository updating the`<workloadNamespace>` placeholder with the namespace where the application will be deployed.

```
tanzu apps workload apply -f frontend/config/workload.yml --local-path . --sub-path frontend --namespace <workloadNamespace>
```

### Spring Cloud Gateway Deployment

Spring Cloud Gateway is used as a "front door" for all requests in the Animal Rescue application.  To deploy the gateway along with applicable routes, run the following commands 
replacing the <workloadNamespace> placeholder with the namespace where the application will be deployed and the <ssoSecret> placeholder with the name of the secret
obtained in the AppSSO installation section

**NOTE** It is optimal (but not necessary) if the workload build and deployment steps above have completed successfully before deploying the gateway routes.

```
ytt -f scgInstance.yml -v workloadNamespace=<workloadNamespace> -v ssoSecret=<ssoSecret>

ytt -f scgRoutes.yml -v workloadNamespace=<workloadNamespace>
```

For example:

```
ytt -f scgInstance.yml -v workloadNamespace=workloads  -v ssoSecret=dbeb6f1a-823a-439a-9a8e-44d2bc631fe0 | kubectl apply -f-

ytt -f scgRoutes.yml -v workloadNamespace=workloads | kubectl apply -f-
```

### Ingress Deployment

The Ingress resources utilizes Contour and will route traffic to the Spring Cloud Gateway.  Run the following command to create the Ingress resources replacing 
`<workloadNamespace>` and `<appDomain>` placeholders with the namespace where the application will be deployed and the application’s DNS domain (the domain name you chose at 
the beginning of these steps).  This will create an Ingress object that uses the full URL of the application (again, the URL you chose at the beginning of these steps).

```
ytt -f ingress.yml -v workloadNamespace=<workloadNamespace> -v appDomainName=<appDomain> | kubectl apply -f-
```

For example:

```
ytt -f ingress.yml -v workloadNamespace=workloads -v appDomainName=perfect300rock.com | kubectl apply -f-
```
