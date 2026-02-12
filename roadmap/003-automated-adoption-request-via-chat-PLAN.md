# Implementation Plan: Automated Adoption Request via Chat

## User Story

> As an authenticated user, I want to tell the chat assistant to adopt a specific animal, so that I can complete the adoption process through a conversation.

## Prerequisites

This story depends on both previous stories being complete:
- **001 - Contextual Sidebar Integration**: Delivers the `ChatSidebar` UI component
- **002 - Natural Language Adoption Inquiry**: Delivers the chat server with LLM integration, MCP tool framework, and the read-only `getAvailableAnimals` tool

This story adds a **write operation** (creating adoption requests) to the chat server's MCP tool set, and introduces **authentication** into the chat flow.

## Current State Analysis (after stories 001 + 002)

**Frontend**:
- `ChatSidebar` streams messages from the chat server via SSE
- `App.js` holds `chatMessages` in state and manages the streaming lifecycle
- `httpClient.js` has `sendChatMessage({ message, history })` calling `POST /chat` on port 8081
- The user's authentication state (`username`, `userStatus`) is tracked in `App.js` via `GET /whoami`
- The existing `submitAdoptionRequest()` in `httpClient.js` calls `POST /animals/{id}/adoption-requests` with cookies (form login session)

**Chat Server** (port 8081, Spring Boot 3.5.10, Spring AI 1.1.2):
- `ChatController` accepts `POST /chat` and returns an SSE stream
- `ChatService` orchestrates LLM calls with tools auto-discovered from the backend MCP server
- Uses `spring-ai-starter-mcp-client` to connect to the backend's MCP server (`spring-ai-starter-mcp-server`)
- The backend's `getAvailableAnimals` tool is auto-discovered -- no local tool definitions in the chat server
- No authentication is configured; the chat server is stateless and unauthenticated

**Backend** (port 8080, Spring Boot 3.5.10, Spring AI 1.1.2):
- `POST /animals/{id}/adoption-requests` requires authentication (Spring Security form login locally, JWT in cloud)
- The endpoint sets `adopterName` from `principal.getName()` -- the adopter identity comes from the server-side session, not the request body
- `SecurityConfiguration` requires authentication for `/whoami` and permits all other exchanges; however, the `POST` endpoint uses `Principal principal` which will be null for unauthenticated requests, causing a NullPointerException
- In local dev, authentication is form-based with session cookies (`alice/test`, `bob/test`)
- The backend is an MCP server (via `spring-ai-starter-mcp-server`) and exposes tools that the chat server discovers automatically

## Key Design Challenge: Authentication Flow

The chat server needs to make **authenticated** requests to the backend on behalf of the logged-in user. There are several options:

### Option A: Token Relay (Chosen)
The frontend sends the user's session cookie (or JWT token) to the chat server, which relays it to the backend when making adoption requests.

- **Pros**: The backend's existing auth model is unchanged; the chat server acts as a transparent proxy for auth
- **Cons**: Requires the chat server to forward credentials; CORS and cookie handling need care

### Option B: Service-to-Service Auth
The chat server authenticates to the backend with its own service credentials and passes the username in the request body.

- **Pros**: Simpler frontend; no credential forwarding
- **Cons**: Requires backend changes (accept username from trusted services); breaks the existing security model

### Option C: Frontend Makes the API Call
The LLM returns a structured "action" payload; the frontend executes the adoption request directly using its existing `submitAdoptionRequest()`.

- **Pros**: No auth changes needed; reuses existing frontend code
- **Cons**: The LLM can't confirm success/failure in its response; requires a two-phase flow

**Decision: Option A (Token Relay)** -- This is the most architecturally clean approach and aligns with how Spring Cloud Gateway already handles token relay in production. For local dev, the frontend will forward its session cookie to the chat server, which relays it to the backend.

## Architecture

```
 Browser (React)                         Chat Server                    Animal Rescue Backend
 +------------------+                    +------------------------+     +--------------------+
 | ChatSidebar      |  POST /chat        | ChatController         |     |                    |
 |                  |  + Cookie/Token     |   |                    |     |                    |
 |                  | -----------------> |   v                    |     |                    |
 |                  |                    | ChatService            |     |                    |
 |                  |                    |   +-- LLM Client       |     |                    |
 |                  |                    |   +-- MCP Tools:       |     |                    |
 |                  |                    |   |   getAnimals() ----+---> | GET /animals       |
 |                  |                    |   |   adoptAnimal() ---+---> | POST /animals/{id} |
 |                  |  SSE stream        |   |     (+ cookie)     |     |   /adoption-reqs   |
 |                  | <----------------- |   |                    |     |   (authenticated)  |
 +------------------+                    +------------------------+     +--------------------+
```

## Scope Decisions

### In Scope
1. New MCP tool: `adoptAnimal(animalId, email, notes)` -- makes an authenticated `POST` to the backend
2. Authentication relay: frontend sends credentials with chat requests; chat server forwards them
3. LLM intent recognition: system prompt updated to handle adoption intents
4. Confirmation/error display: LLM reports success or failure in its streamed response
5. Conversational data gathering: if the user says "I want to adopt Chocobo" without providing email/notes, the LLM asks follow-up questions before calling the tool
6. Security configuration for the chat server (CORS, cookie forwarding)

### Out of Scope
- Edit/delete adoption requests via chat (can be a follow-up story)
- Payment processing or legal agreements
- Multi-animal adoption in a single conversation turn

## New Files

| File | Purpose |
|------|---------|
| `chat-server/src/main/java/.../security/ChatServerSecurityConfig.java` | CORS config, cookie/token handling |
| `backend/src/main/java/.../mcp/AdoptionMcpTools.java` | `adoptAnimal` MCP tool definition (lives in the backend since the backend is the MCP server) |

## Modified Files

| File | Change |
|------|--------|
| `chat-server/src/main/java/.../ChatController.java` | Extract auth credentials from request, pass to `ChatService` |
| `chat-server/src/main/java/.../ChatService.java` | Pass auth context to MCP tools; update system prompt for adoption intents |
| `chat-server/src/main/java/.../config/ChatServerConfig.java` | Configure `WebClient` to forward cookies |
| `chat-server/src/main/resources/application.yml` | CORS allowed origins |
| `frontend/src/httpClient.js` | Add `credentials: 'include'` to `sendChatMessage` fetch call |
| `frontend/src/App.js` | Pass `username` to `ChatSidebar` for display context |
| `frontend/src/components/chat-sidebar.js` | Show login prompt if user tries to adopt while unauthenticated |
| `e2e/cypress/e2e/rescue.cy.js` | Add tests for chat-based adoption flow |
| `backend/src/main/java/.../security/SecurityConfiguration.java` | Add CORS config to allow chat server origin for cookie-based requests |

## Detailed Implementation Steps

### Step 1: Configure Authentication Relay

**Frontend -- `httpClient.js`:**

Update `sendChatMessage` to include credentials:
```js
export function sendChatMessage({ message, history }) {
    return fetch(`${chatServerBaseUrl}/chat`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',  // Forward session cookie
        body: JSON.stringify({ message, history }),
    });
}
```

**Backend -- `SecurityConfiguration.java`:**

Add CORS configuration to allow the chat server to relay cookies:
```java
.authorizeExchange(authorizeExchangeSpec -> {
    authorizeExchangeSpec
        .pathMatchers("/whoami").authenticated()
        .anyExchange().permitAll();
})
// Add CORS for chat server relay
.cors(corsSpec -> {
    corsSpec.configurationSource(exchange -> {
        var config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
            "http://localhost:3000",
            "http://localhost:8081"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE"));
        config.setAllowCredentials(true);
        config.setAllowedHeaders(List.of("*"));
        return config;
    });
})
```

**Chat Server -- `ChatServerSecurityConfig.java`:**
```java
@Configuration
public class ChatServerSecurityConfig {

    @Bean
    public CorsWebFilter corsFilter() {
        var config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000"));
        config.setAllowedMethods(List.of("POST", "OPTIONS"));
        config.setAllowCredentials(true);
        config.setAllowedHeaders(List.of("*"));

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return new CorsWebFilter(source);
    }
}
```

### Step 2: Create the Adoption MCP Tool (in the Backend)

Since the backend is the MCP server (via `spring-ai-starter-mcp-server`), the `adoptAnimal` tool is defined **in the backend**, not the chat server. The chat server's MCP client will auto-discover it alongside the existing `getAvailableAnimals` tool.

**`backend/src/main/java/.../mcp/AdoptionMcpTools.java`:**

```java
@Component
public class AdoptionMcpTools {

    private final AdoptionRequestRepository adoptionRequestRepository;
    private final AnimalRepository animalRepository;

    public AdoptionMcpTools(AdoptionRequestRepository adoptionRequestRepository,
                            AnimalRepository animalRepository) {
        this.adoptionRequestRepository = adoptionRequestRepository;
        this.animalRepository = animalRepository;
    }

    @Tool(description = "Submit an adoption request for a specific animal. " +
          "Requires the animal's ID, the adopter's name, email address, and optional notes. " +
          "Returns success or an error message.")
    public Mono<String> adoptAnimal(
            @ToolParam(description = "The numeric ID of the animal to adopt") Long animalId,
            @ToolParam(description = "The name of the adopter") String adopterName,
            @ToolParam(description = "The adopter's contact email address") String email,
            @ToolParam(description = "Optional notes about why the user wants to adopt") String notes
    ) {
        return animalRepository.findById(animalId)
            .flatMap(animal -> {
                AdoptionRequest request = new AdoptionRequest();
                request.setAdopterName(adopterName);
                request.setEmail(email);
                request.setNotes(notes != null ? notes : "");
                request.setAnimal(animalId);
                return adoptionRequestRepository.save(request)
                    .map(saved -> "Adoption request submitted successfully for " + animal.getName() + "!");
            })
            .switchIfEmpty(Mono.just("Error: Animal with id " + animalId + " doesn't exist!"));
    }
}
```

Because the tool runs inside the backend process, it has direct access to the repositories -- no HTTP calls or cookie forwarding needed for the tool itself. Authentication is handled at the chat server level (see Step 3).

### Step 3: Update the Chat Controller to Pass Auth Context

The controller extracts the session cookie from the incoming request and makes it available to the MCP tools:

```java
@PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<String> chat(
        @RequestBody ChatRequest request,
        @CookieValue(name = "SESSION", required = false) String sessionCookie
) {
    return chatService.chat(
        request.message(),
        request.history(),
        sessionCookie  // Passed through to MCP tools
    );
}
```

### Step 4: Update the Chat Service System Prompt

Extend the system prompt to handle adoption intents:

```java
this.chatClient = chatClientBuilder
    .defaultSystem("""
        You are a friendly assistant for the Animal Rescue center.
        You help potential adopters find animals and complete adoptions.

        CAPABILITIES:
        - Use getAvailableAnimals to look up animals when users ask about them.
        - Use adoptAnimal to submit adoption requests when users want to adopt.

        ADOPTION FLOW:
        1. When a user expresses intent to adopt (e.g. "I want to adopt Chocobo"),
           first confirm the animal name and look up its ID using getAvailableAnimals.
        2. Ask for their contact email if not already provided.
        3. Ask if they'd like to add any notes (optional).
        4. Call adoptAnimal with the gathered information.
        5. Report the result -- success or error -- clearly.

        IMPORTANT:
        - If the user is not logged in (adoption fails with auth error),
          tell them they need to sign in first using the button in the top-right corner.
        - Never fabricate animal IDs. Always look up the real ID from getAvailableAnimals.
        - Be conversational and friendly. Use the animal's name, not just its ID.
        """)
    .build();
```

### Step 5: Update the Chat Sidebar for Auth Awareness

Pass the `username` from `App.js` to `ChatSidebar`:

```jsx
<ChatSidebar
    isOpen={this.state.isSidebarOpen}
    messages={this.state.chatMessages}
    onSendMessage={(text) => this.addChatMessage(text)}
    onClose={() => this.toggleSidebar()}
    onOpen={() => this.toggleSidebar()}
    username={this.state.username}
/>
```

In `ChatSidebar`, if the user is not signed in and types a message that looks like an adoption intent, show a subtle inline hint: "Sign in to adopt animals through chat." This is a soft guardrail; the real enforcement happens server-side.

### Step 6: Refresh Animal Cards After Chat Adoption

When the chat server confirms a successful adoption, the animal cards on the main page should reflect the new adoption request. Update `App.js`:

```js
async addChatMessage(text) {
    // ... existing streaming logic ...

    // After stream completes, refresh animals if the response
    // indicates a successful adoption
    if (assistantMsg.text.includes('successfully')) {
        this.fetchAnimals();
    }
}
```

This is a simple heuristic. A more robust approach would be to have the chat server return structured metadata alongside the streamed text, but that adds complexity beyond what this story requires.

### Step 7: Write Tests

**Backend -- `AdoptionMcpToolsTest.java`:**

Since the `adoptAnimal` tool lives in the backend (the MCP server), its tests belong in the backend test suite:

```java
@Test
void adoptAnimal_succeeds() {
    String result = tools.adoptAnimal(1L, "alice", "test@email.com", "Love this cat!")
        .block();

    assertThat(result).contains("successfully");
}

@Test
void adoptAnimal_failsWhenAnimalNotFound() {
    String result = tools.adoptAnimal(999L, "alice", "test@email.com", "notes")
        .block();

    assertThat(result).contains("Error");
    assertThat(result).contains("doesn't exist");
}
```

**E2E (Cypress):**
```js
describe('adoption via chat', () => {
    before(() => {
        cy.login('alice', 'test');
    });

    it('adopts an animal through chat conversation', () => {
        cy.get('.chat-fab').click();

        // Express intent
        cy.get('.chat-sidebar-input input')
            .type('I would like to adopt Chocobo{enter}');

        // LLM should ask for email (wait for streaming response)
        cy.get('.chat-sidebar-messages', { timeout: 15000 })
            .should('contain.text', 'email');

        // Provide email
        cy.get('.chat-sidebar-input input')
            .type('alice@example.com{enter}');

        // LLM should confirm or ask for notes, then submit
        cy.get('.chat-sidebar-messages', { timeout: 15000 })
            .should('contain.text', 'success')
            .or('contain.text', 'submitted');

        // Verify the animal card reflects the new adoption
        cy.get('.chat-sidebar-header .close.icon').click();
        cy.get('.ui.card').first().find('.pending-number')
            .invoke('text')
            .then(text => {
                expect(parseInt(text)).to.be.greaterThan(0);
            });
    });

    it('shows error when unauthenticated user tries to adopt', () => {
        // Log out first
        cy.contains('Sign out').click();
        cy.contains('Log Out').click();
        cy.visit('/');

        cy.get('.chat-fab').click();
        cy.get('.chat-sidebar-input input')
            .type('I want to adopt Sam{enter}');

        cy.get('.chat-sidebar-messages', { timeout: 15000 })
            .should('contain.text', 'sign in')
            .or('contain.text', 'log in');
    });
});
```

### Step 8: Verify Full Test Suite

```bash
# Backend tests (unchanged, should still pass)
./gradlew :backend:test

# Chat server tests
./gradlew :chat-server:test

# Full E2E
./scripts/local.sh start --quiet
./scripts/local.sh e2e --quiet
./scripts/local.sh stop
```

## Acceptance Criteria Mapping

| Criteria | How It's Met |
|----------|-------------|
| Intent Recognition: LLM identifies "adopt" intent and extracts animalId/name | System prompt defines the adoption flow. LLM calls `getAvailableAnimals` to resolve name to ID, then calls `adoptAnimal` with the correct ID. |
| API Action: MCP server triggers adoption request creation | `AdoptionMcpTools.adoptAnimal()` is defined in the backend (MCP server) and has direct repository access. The chat server's MCP client invokes it via the MCP protocol. The tool creates the adoption request directly in the database. |
| Security: Chat relays user's SSO/JWT token | Frontend sends `credentials: 'include'` with the chat request. Chat server extracts the user identity from the session and passes it as a parameter to the `adoptAnimal` MCP tool. The tool runs in the backend with direct repository access, so the adopter name is set from the relayed identity. |
| Confirmation: Chat displays success or failure | The `adoptAnimal` tool returns a human-readable result string. The LLM incorporates this into its streamed response. Errors (auth failure, invalid animal) are reported clearly. |

## Risks and Mitigations

| Risk | Mitigation |
|------|-----------|
| Cookie forwarding across origins (CORS) | Explicit CORS config on both backend and chat server. `SameSite=Lax` cookies work for same-site requests in local dev. For production behind a gateway, all services share the same origin. |
| LLM calls adoptAnimal prematurely (without email) | System prompt explicitly requires gathering email before calling the tool. The tool parameter `email` is required. If the LLM omits it, the backend will reject the request (validation). |
| LLM hallucinates an animal ID | System prompt requires looking up the real ID via `getAvailableAnimals` first. The backend returns 400 for non-existent IDs, and the tool surfaces this error. |
| Session cookie name varies by environment | Local dev uses Spring's default `SESSION` cookie. Cloud deployments use JWT tokens via `Authorization` header. The chat server should support both (check for cookie first, then header). |
| Adoption succeeds but animal cards don't refresh | `App.js` calls `fetchAnimals()` after detecting a successful adoption in the streamed response. As a fallback, the user can manually refresh. |
| Multi-turn conversation state | The full chat history is sent with each request, so the LLM has context from previous turns (e.g. the user said "Chocobo" three messages ago). History size should be capped (e.g. last 20 messages) to stay within token limits. |
| Rate limiting / abuse | The chat server should rate-limit adoption tool calls (e.g. max 5 per minute per session) to prevent accidental duplicate submissions. |

## Estimated Effort

| Task | Estimate |
|------|----------|
| Step 1: Authentication relay (frontend + backend CORS + chat server) | 1 hour |
| Step 2: Adoption MCP tool | 45 min |
| Step 3: Chat controller auth context | 30 min |
| Step 4: System prompt update | 30 min |
| Step 5: Sidebar auth awareness | 20 min |
| Step 6: Animal card refresh after adoption | 20 min |
| Step 7: Tests (server + e2e) | 1.5 hours |
| Step 8: Full integration test run | 30 min |
| **Total** | **~5.5 hours** |

## Cumulative Effort Across All Three Stories

| Story | Estimate |
|-------|----------|
| 001 - Contextual Sidebar Integration | ~2.5 hours |
| 002 - Natural Language Adoption Inquiry | ~5.5 hours |
| 003 - Automated Adoption Request via Chat | ~5.5 hours |
| **Total** | **~13.5 hours** |
