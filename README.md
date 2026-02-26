# Animal Rescue

Sample app for Spring Cloud Gateway, enhanced with AI-powered chat using Spring AI and Model Context Protocol (MCP). Originally built to demonstrate gateway routing and SSO, this branch adds an AI assistant that can browse animals, submit adoptions, and query pending adopters through natural language.

## Features

### Gateway & SSO
- Routing traffic to configured internal routes with container-to-container networking
- Gateway routes configured through service bindings
- Simplified route configuration
- SSO login and `ClaimHeader` filter to forward user identity to backend services
- Circuit breaker filter

### AI Chat (Spring AI + MCP)
- Conversational chat sidebar powered by Spring AI and OpenAI-compatible models
- MCP server on the backend exposing tools: `getAvailableAnimals`, `adoptAnimal`, `getPendingAdopters`
- MCP client in the chat-server that discovers and invokes backend tools via Streamable HTTP transport
- Authentication-aware: in cloud, the gateway forwards user identity via `ClaimHeader`; locally, the chat server resolves the user via session cookie
- Rate limiting on the `/chat` endpoint with friendly 429 handling in the UI

## Architecture

The application consists of three services:

| Service | Port | Description |
|---|---|---|
| **backend** | 8080 | Spring WebFlux API with H2 database, Spring Security, and MCP server |
| **chat-server** | 8081 | Spring AI chat service with MCP client that connects to the backend |
| **frontend** | 3000 | React app with animal cards, adoption workflow, and chat sidebar |

```
┌──────────────┐     ┌──────────────────┐     ┌──────────────────┐
│   Frontend   │────▶│   Chat Server    │────▶│     Backend      │
│  (React)     │     │ (Spring AI +     │ MCP │ (WebFlux + MCP   │
│  port 3000   │     │  MCP Client)     │     │  Server + H2)    │
│              │     │  port 8081       │     │  port 8080       │
└──────────────┘     └──────────────────┘     └──────────────────┘
        │                                             ▲
        └─────────────────────────────────────────────┘
                    REST API (animals, adoptions, auth)
```

## Tech Stack

- **Spring Boot 3.5.10** with Java 17
- **Spring AI 1.1.2** (OpenAI starter, MCP client & server)
- **Spring WebFlux** (reactive backend and chat server)
- **Spring Security** (form login locally, OAuth2/JWT on Cloud Foundry)
- **H2** (embedded database via R2DBC)
- **React** with Semantic UI

## Table of Contents

* [Development](#development)
* [Deploy to Tanzu Application Service](#deploy-to-tanzu-application-service)
* [AI Chat Configuration](#ai-chat-configuration)
* [Gateway Frontend Config](#gateway-frontend-config)
* [Application Walkthrough](#application-walkthrough)

## Development

### Prerequisites

- Java 17+
- Node.js (for the frontend)
- An OpenAI-compatible API endpoint (set via environment variables)

### Run locally

```bash
./scripts/local.sh start         # start backend, chat-server, and frontend
./scripts/local.sh start --quiet # start everything without opening browser; output goes to ./scripts/out/
./scripts/local.sh stop          # stop all services
```

### Local security configuration

The backend uses form login for local development with two test accounts: `alice / test` and `bob / test`.

In a real deployment with Spring Cloud Gateway on Tanzu Platform, OAuth2 login is managed by the gateway and the `ClaimHeader` filter forwards the authenticated user's JWT claims (e.g. `user_name`, `sub`) as HTTP headers (`X-User-Name`, `X-User-Sub`) to backend services.

### Tests

```bash
./scripts/local.sh init          # install frontend and e2e dependencies
./scripts/local.sh ci            # run backend tests and e2e tests
./scripts/local.sh backend       # run backend tests only
./scripts/local.sh e2e --quiet   # run e2e tests headlessly
```

For interactive e2e testing:

```bash
./scripts/local.sh start
./scripts/local.sh e2e
```

E2e test output videos are saved to `./e2e/cypress/videos/`. See the [Cypress API docs](https://docs.cypress.io/api/api/table-of-contents.html) for more information.

## Deploy to Tanzu Application Service

```bash
./scripts/cf_deploy init    # install dependencies and build artifacts
./scripts/cf_deploy deploy  # deploy frontend, backend, chat-server, and gateway
```

Visit `https://gateway-demo.${appsDomain}/rescue` to view the app.

To tear down:

```bash
./scripts/cf_deploy destroy
```

Other useful commands:

```bash
./scripts/cf_deploy push                          # build and push frontend and backend
./scripts/cf_deploy dynamic_route_config_update   # update bound apps' route config
./scripts/cf_deploy rebind                        # unbind and rebind services
./scripts/cf_deploy upgrade                       # upgrade the gateway instance
```

Gateway configuration files:

- Gateway instance config: `./gateway/api-gateway-config.json`
- Frontend route config: `./frontend/api-route-config.json`
- Backend route config: `./backend/api-route-config.json`
- Chat server route config: `./chat-server/api-route-config.json`

## AI Chat Configuration

The chat-server requires an OpenAI-compatible endpoint. Configure it via environment variables or `chat-server/src/main/resources/application.yml`:

| Variable | Description | Default |
|---|---|---|
| `OPENAI_BASE_URL` | Base URL of the OpenAI-compatible API | _(none)_ |
| `OPENAI_API_KEY` | API key for the model endpoint | _(none)_ |
| `ANIMAL_RESCUE_BACKEND_URL` | URL of the backend for MCP tool discovery | `http://localhost:8080` |

The chat-server connects to the backend's MCP server over Streamable HTTP to discover and invoke tools (`getAvailableAnimals`, `adoptAnimal`, `getPendingAdopters`). On Cloud Foundry, the model endpoint is provided via a bound service (`animal-rescue-model`).

### MCP Tools

The backend exposes three MCP tools:

- **`getAvailableAnimals`** — Returns all animals with their details and current adoption requests. No authentication required.
- **`adoptAnimal`** — Submits an adoption request for a given animal ID with adopter name, email, and notes. Requires an authenticated user.
- **`getPendingAdopters`** — Lists pending adoption requests for a named animal. Requires an authenticated user.

## Gateway Frontend Config

The frontend is a React SPA pushed with a static buildpack. Key configuration points:

1. `homepage` in `package.json` is set to `/rescue`, matching the gateway route path, so all assets are served under `/rescue`.
2. The "Sign in to adopt" button links to `/rescue/login`, an SSO-enabled gateway path that triggers the OAuth2 flow and redirects back to `/rescue`.
3. `REACT_APP_BACKEND_BASE_URI` is set to `/backend`, matching the backend's gateway route path.

## Application Walkthrough

Visit the app to see animal bios with `Adopt` buttons (disabled until you sign in).

Click **Sign in to adopt** to authenticate. Once logged in, you'll see a greeting and enabled `Adopt` buttons.

Click **Adopt** on any animal card to submit an adoption request with your email and notes.

Open the **chat sidebar** (bottom-right bubble) to interact with the AI assistant. You can:
- Ask about available animals
- Request to adopt an animal through conversation
- Query pending adopters for a specific animal (when signed in)

The assistant enforces authentication — unauthenticated users can browse but cannot adopt or view pending requests.

## CI

### GitHub Actions

GitHub Actions run all checks for the `main` branch and pull requests. Workflow configuration is in `.github/workflows`.
