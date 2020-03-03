# Animal Rescue ♥️😺 ♥️🐶 ♥️🐰 ♥️🐦 ♥️🐹
![Test All](https://github.com/spring-cloud-services-samples/animal-rescue/workflows/Test%20All/badge.svg?branch=master)

Sample app for Tanzu Spring Cloud Gateway tile. 
Features we demonstrate with this sample app:
- Routing traffic to configured internal routes with container-to-container network
- Gateway routes configured through service bindings
- Simplified route configuration
- SSO login and token relay on behalf of the routed services
- (Coming soon) Required scopes on routes

## Deploy to CF

Run the following scripts to set up everything:
```bash
./scripts/cf_deploy init    # installs dependencies and builds the deployment artifact
./scripts/cf_deploy deploy  # handles everything you need to deploy the frontend, backend, and gateway
```
Then visit the the frontend url `https://gateway-demo.${appsDomain}/rescue` to view the sample app.

Once you have enough fun with the sample app, run the following script to clean up the environment:
```bash
./scripts/cf_deploy destroy # tears down everything
```

Some other commands that might be helpful:
```bash
./scripts/cf_deploy push    # builds and pushes frontend and backend
./scripts/cf_deploy rebind  # unbinds and rebinds frontend and backend
```

## Special frontend config related to gateway

The frontend application is implemented in ReactJS, and is pushed with static buildpack. Because of it's static nature, we had to do the following 
1. `homepage` in `package.json` is set to `/rescue`, which is the path we set for the frontend application in gateway config (`frontend/gateway-config.json`). This is to make sure all related assets is requested under `/rescue` path as well.
1. `Sign in to adopt` button is linked to `/rescue/login`, which is a path that is `sso-enabled` in gateway config (`frontend/gateway-config.json`). This is necessary for frontend apps bound to a sub path on gateway because the Oauth2 login flow redirects users to the original requested location or back to `/` if no saved request exists. This setting is not necessary if the frontend app is bound to path `/`.
1. `REACT_APP_BACKEND_BASE_URL` is set to `/backend` in build script, which is the path we set for the backend application in gateway config (`backend/gateway-config.json`). This is to make sure all our backend API calls are appended with the `backend` path.


## Try it out
Visit `https://gateway-demo.${appsDomain}/rescue`, you should see cute animal bios with the `Adopt` buttons disabled. All the information are fetched from a public `GET` backend endpoint `/animals`. 
![homepage](./docs/images/homepage.png)

Click the `Sign in to adopt` button on the top right corner, you should be redirected to the SSO login page if you haven't already logged in to SSO.
![log in page](./docs/images/login.png)

Once you logged in, you should see a greeting message regarding the username you log in with on the top right corner, and the `Adopt` buttons should be enabled.
![logged in view](./docs/images/logged-in.png)

Click on the `Adopt` button, input your contact email and application notes in the model, then click `Apply`, a `POST` request should be sent to a `sso-enabled` backend endpoint `/animals/{id}/adoption-requests`, with the adopter set to your username we parsed from your token.
![adopt model](./docs/images/adopt.png)   

Then the model should close, and you should see the `Adopt` button you clicked just now has turned into `Edit Adoption Request`. This is matched by your SSO log in username.
![adopted view](./docs/images/adopted.png)   

Click on the `Edit Adoption Request` again, you can view, edit (`PUT`), and delete (`DELETE`) the existing request.
![view or edit existing adoption request model](./docs/images/edit-or-delete.png)   

**Note**
Documentation may get out of date. Please refer to the [e2e test](./e2e/cypress/integration/) and the test output video for the most accurate user flow description.

## Development

#### Run locally 
Use the following commands to manage the local lifecycle of animal-rescue
```bash
./scripts/local.sh start         # start auth server, frontend app, and backend app
./scripts/local.sh start --quiet # start everything without launching the app in browser, and redirects all output to `./scripts/out/`
./scripts/local.sh stop          # stop auth server, frontend app, and backend app
./scripts/local.sh cleanup       # remove the uaa server docker image
``` 

#### Local security configuration
Backend uses Form login for local development with two test accounts - `alice / test` and `bob / test`. 
Note that in a real deployment with Gateway, OAuth2 login will be managed by the gateway itself, and your app should use `TokenRelay` filter to receive OpenID ID Token in `Authorization` header. See `CloudFoundrySecurityConfiguration` class for an example of Spring Security 5 configuration to handle token relay correctly.

> It is also possible to use OAuth2 login flow for the app. This requires running an authorization server locally. See `local-oauth2-flow` for an example of using Cloud Foundry User Account and Authentication (UAA) running in a Docker container locally.

#### Tests
Execute the following script to run all tests:
```bash
./scripts/local.sh init          # install dependencies for the frontend folder and the e2e folder
./scripts/local.sh ci            # run backend tests and e2e tests
./scripts/local.sh backend       # run backend test only
./scripts/local.sh e2e --quiet   # run e2e test only without interactive mode
```

You can find an e2e test output video showing the whole journey in `./e2e/cypress/videos/` after the test run. 
If you would like to launch the test in an actual browser and run e2e test interactively, you may run the following commands:
```bash
./scripts/local.sh start
./scripts/local.sh e2e
``` 
More detail about the e2e testing framework can be found at [cypress api doc](https://docs.cypress.io/api/api/table-of-contents.html) 
