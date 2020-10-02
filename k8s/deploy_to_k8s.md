## Deploy to Kubernetes

> :warning: Spring Cloud Gateway for k8s is under active development, everything in this folder is experimental.  

Make sure you have Spring Cloud Gateway for k8s installed.

If you have `kustomize` installed, you can run the following command:

```bash
kustomize build ./k8s | kubectl apply -f -
```

If you don't want to use `kustomize`, you can apply each yaml file in the `k8s` folder manually into the `animal-rescue` namespace (or any namespace you like!).

There are two gateway instance being created - `gateway-demo` and `gateway-demo-with-dynamic-routes`. 
* `gateway-demo` has all the routes pre-defined when creating the gateway. 
* `gateway-demo-with-dynamic-routes` doesn't have any routes defined on creation. The route definition is defined in a `SpringCloudGatewayBinding` object that can be version-controlled with each routed application.

After applying all the manifest files, there should be a SCG deployment and a `ClusterIP` service for each gateway instance deployed in the SCG installation namespace (`spring-cloud-gateway` by default).

Expose your gateway instance in your favorite way, e.g. ingress or port forwarding, then access `/rescue` path` to view the animal-rescue app.
