# Animal Rescue ♥️😺 ♥️🐶 ♥️🐰

Sample app for Tanzu Spring Cloud Gateway tile. 
Features we demonstrate with this sample app:
- Routing traffic to configured internal routes with container-to-container network
- Gateway routes configured through service bindings
- Simplified route configuration
- (WIP) SSO login and access control on behalf of the routed services

### Deploy to CF

Run the following scripts to set up everything:
```bash
./scripts/cf_deploy build # builds the deployment artifact
./scripts/cf_deploy deploy # handles everything you need to deploy the frontend, backend, and gateway
```
Then visit the the frontend url `https://gateway-demo.${appsDomain}/rescue` to view the sample app.

Once you have enough fun with the sample app, run the following script to clean up the environment:
```bash
./scripts/cf_deploy destroy
```

Some other commands that might be helpful:
```bash
./scripts/cf_deploy push # builds and pushes frontend and backend
./scripts/cf_deploy rebind # unbinds and rebinds frontend and backend
```

**Special frontend config related to gateway**

The frontend application is implemented in ReactJS, and is pushed with static buildpack. Because of it's static nature, we had to do the following 
1. `homepage` in `package.json` is set to `/rescue`, which is the path we set for the frontend application in gateway config (`frontend/gateway-config.json`). This is to make sure all related assets is requested under `/rescue` path as well.
1. `REACT_APP_BACKEND_BASE_URL` is set to `/backend`, which is the path we set for the backend application in gateway config (`backend/gateway-config.json`). This is to make sure all our backend API calls are appended with the `backend` path.

### Run locally
Start backend app:
```bash
cd backend
./gradlew bootRun
```
Start frontend app:
```bash
cd frontend
npm install
npm start
```

## Try it out
[Add UI descriptions and screenshots]
