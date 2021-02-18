## Deploy to Kubernetes

> :warning: Spring Cloud Gateway for k8s is under active development, everything in this folder is experimental.  

Make sure you have Spring Cloud Gateway for k8s installed.

The JSON Web Key Set (JWKS) is a set of keys containing the public keys used to verify any JSON Web Token (JWT) issued by the authorization server and signed using the RS256 signing algorithm.

For Animal Rescue sample Single Sign-On (SSO) to work, you will need to provide two secret files:
* ./k8s/base/secrets/sso-credentials-for-backend.txt
* ./k8s/overlays/sso-secret-for-gateway/secrets/test-sso-credentials.txt

Before you start, and for validation, please locate the JWKS endpoint info `jwks_uri` from your provider.
The endpoint exists at:
```
https://YOUR_DOMAIN/.well-known/openid-configuration

# ex: for Okta, for the configured Issuer URI, you can retrieve it at:
https://<issuer-uri>/.well-known/openid-configuration

$ curl https://dev-1234567.okta.com/oauth2/abcd12345/.well-known/openid-configuration

{
  "issuer": "https://dev-1234567.okta.com/oauth2/abcd12345",
...
  "jwks_uri": "https://dev-1234567.okta.com/oauth2/abcd12345/v1/keys",
....

# Please note that the format used by Okta is jwks_uri="<issuer-uri>/v1/keys"
```

SSO file configuration:
* ./k8s/base/secrets/sso-credentials-for-backend.txt
  ```
  jwk-set-uri=<jwks_uri>
  
  ex.: 
  jwks_uri=https://dev-1234567.okta.com/oauth2/abcd12345/v1/keys
  ``` 
* ./k8s/overlays/sso-secret-for-gateway/secrets/test-sso-credentials.txt
  ```
  scope=openid,profile,email
  client-id={your_client_id}
  client-secret={your_client_secret}
  issuer-uri={your_issuer_uri}
  ```
  Please note: this file structure may change as the product evolves. Please refer to the SCG4K8s doc for the most up-to-date template.
  
### Deploy with Kustomize
If you have `kustomize` installed, you can run the following command:

```bash
kustomize build ./k8s | kubectl apply -f -
```

### Deploy with Kubectl
If you don't want to use `kustomize`, you can apply each yaml file in the `k8s` folder manually into the `animal-rescue` namespace (or any namespace you like!). 

Make sure to create the SSO credentials secret in the SCG installation namespace (`spring-cloud-gateway` by default).

A gateway instance is created, named `gateway-demo`, and it doesn't have any API routes defined on creation. The API route definitions are defined in a `SpringCloudGatewayBinding` object that can be version-controlled with each routed application. 

After applying all the manifest files, there should be a SCG deployment and a `ClusterIP` service for each gateway instance deployed in the SCG installation namespace (`spring-cloud-gateway` by default). 

Expose your gateway instance in your favorite way, e.g. ingress or port forwarding, then access `/rescue` path to view the animal-rescue app.

`Example`: if you have installed the `animal-rescue` app in the `animal-rescue` namespace, and you wish to simply access the app quickly, you can validate the deployment and port-forward the `gateway-demo` service:
```shell
$ kubectl get all -n animal-rescue
NAME                                          READY   STATUS    RESTARTS   AGE
pod/animal-rescue-backend-757bb96466-pzr4l    1/1     Running   0          21h
pod/animal-rescue-frontend-5c6f9bfc9b-kgxzg   1/1     Running   0          21h
pod/gateway-demo-0                            1/1     Running   0          22h
pod/gateway-demo-1                            1/1     Running   0          21h

NAME                             TYPE        CLUSTER-IP    EXTERNAL-IP   PORT(S)    AGE
service/animal-rescue-backend    ClusterIP   10.0.25.68    <none>        80/TCP     7d18h
service/animal-rescue-frontend   ClusterIP   10.0.31.179   <none>        80/TCP     7d18h
service/gateway-demo             ClusterIP   10.0.23.250   <none>        80/TCP     7d18h
service/gateway-demo-hazelcast   ClusterIP   None          <none>        5701/TCP   7d18h

NAME                                     READY   UP-TO-DATE   AVAILABLE   AGE
deployment.apps/animal-rescue-backend    1/1     1            1           7d18h
deployment.apps/animal-rescue-frontend   1/1     1            1           7d18h

NAME                                                DESIRED   CURRENT   READY   AGE
replicaset.apps/animal-rescue-backend-757bb96466    1         1         1       7d18h
replicaset.apps/animal-rescue-frontend-5c6f9bfc9b   1         1         1       7d18h

# port-forward the gateway-demo service
$ kubectl -n=animal-rescue  port-forward service/gateway-demo 8080:80

# animal-rescue is available at
http://localhost:8080/rescue
```