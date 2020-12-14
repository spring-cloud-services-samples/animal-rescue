## Deploy to Kubernetes

> :warning: Spring Cloud Gateway for k8s is under active development, everything in this folder is experimental.  

Make sure you have Spring Cloud Gateway for k8s installed.

For Animal Rescue sample Single Sign-On (SSO) to work, you will need to provide two secret files:

* ./k8s/base/secrets/sso-credentials-for-backend.txt
  ```
  jwk-set-uri=https://{your_auth_domain}/{.well-known/jwks.json}
  ``` 
* ./k8s/overlays/sso-secret-for-gateway/secrets/test-sso-credentials.txt
  ```
  scope=openid,profile,email
  client-id={your_client_id}
  client-secret={your_client_secret}
  authorization-grant-type=authorization_code
  issuer-uri={your_issuer_uri}
  provider=sso
  ```
  This file structure may change as the product evolves. Please refer to the SCG4K8s doc for the most up-to-date template.
  
If you have `kustomize` installed, you can run the following command:

```bash
kustomize build ./k8s | kubectl apply -f -
```

If you don't want to use `kustomize`, you can apply each yaml file in the `k8s` folder manually into the `animal-rescue` namespace (or any namespace you like!). Make sure to create the SSO credentials secret in the SCG installation namespace (`spring-cloud-gateway` by default).

A gateway instance is created named `gateway-demo` that doesn't have any API routes defined on creation. The API route definitions are defined in a `SpringCloudGatewayBinding` object that can be version-controlled with each routed application. After applying all the manifest files, there should be a SCG deployment and a `ClusterIP` service for each gateway instance deployed in the SCG installation namespace (`spring-cloud-gateway` by default). Expose your gateway instance in your favorite way, e.g. ingress or port forwarding, then access `/rescue` path to view the animal-rescue app.

