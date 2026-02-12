# Implementation Plan: Natural Language Adoption Inquiry

## User Story

> As an interested adopter, I want to ask the chat assistant about available animals, so that I can find a pet that fits my needs without manually filtering the animal list.

## Prerequisites

This story depends on **001 - Contextual Sidebar Integration** being complete. That story delivers the `ChatSidebar` component with a message list, input form, and FAB toggle. The sidebar currently uses a placeholder auto-reply. This story replaces that placeholder with a real LLM-backed chat server that can query the Animal Rescue backend via MCP (Model Context Protocol).

## Current State Analysis

**Frontend** (after story 001):
- `ChatSidebar` component renders messages and calls `App.addChatMessage(text)` on send
- `App.js` holds `chatMessages` array in state; currently appends a hardcoded placeholder reply
- `httpClient.js` has `getAnimals()` which calls `GET /animals` and returns the full list

**Backend**:
- `GET /animals` returns all 10 animals with their adoption requests (public, no auth required)
- Animal fields: `id`, `name`, `rescueDate`, `avatarUrl`, `description`, `adoptionRequests[]`
- No species/breed/size fields exist in the schema -- the LLM will need to infer attributes from the `description` text and `avatarUrl` (which contains breed hints in the URL path)
- The backend is a Spring Boot 3.3.5 WebFlux app running on port 8080

**Infrastructure**:
- No chat server or MCP server exists yet
- No LLM dependency exists in the project

## Architecture Overview

```
 Browser (React)                    Chat Server (Spring Boot)             Animal Rescue Backend
 +-----------------+                +-------------------------+           +--------------------+
 | ChatSidebar     |  POST /chat    | ChatController          |           | AnimalController   |
 | (websocket or   | ------------> | ChatService             |           |                    |
 |  HTTP SSE)      | <-----------  |   +-- LLM Client        |  MCP      | GET /animals       |
 |                 |  streamed     |   +-- MCP Client -------+---------> | POST /animals/     |
 |                 |  response     |       (tool calls)       |  HTTP     |   {id}/adoption-   |
 +-----------------+               +-------------------------+           |   requests         |
                                                                          +--------------------+
```

The **Chat Server** is a new Spring Boot application that:
1. Receives user messages from the frontend
2. Sends them to an LLM (e.g. OpenAI, Anthropic, or a local model) with MCP tool definitions
3. The LLM decides when to call the `GET /animals` tool
4. The MCP client executes the tool call against the Animal Rescue backend
5. The LLM uses the returned animal data to compose a natural-language response
6. The response is streamed back to the frontend

### Why a Separate Chat Server?

- **Separation of concerns**: The Animal Rescue backend is a domain API; the chat server is an orchestration layer
- **LLM API keys stay server-side**: Never exposed to the browser
- **MCP protocol**: The chat server acts as an MCP client calling the Animal Rescue backend as an MCP-compatible tool server (or wraps the REST API as MCP tools)
- **Streaming**: The chat server can stream LLM responses via SSE without modifying the existing backend

## Scope Decisions

### In Scope
1. A new **chat-server** Spring Boot application in the repository
2. MCP tool definition for `GET /animals` (read-only, public)
3. LLM integration with streaming responses
4. Frontend updates to connect to the chat server via SSE (Server-Sent Events)
5. Markdown rendering in chat messages
6. Context-aware filtering (the LLM interprets user queries against animal data)

### Out of Scope
- Authenticated actions (adoption requests via chat) -- that is story 003
- Chat history persistence (database/localStorage)
- Multi-turn memory beyond the current browser session
- Fine-tuning or custom model training

## New Files

| File | Purpose |
|------|---------|
| `chat-server/build.gradle` | Spring Boot 3.3.5 app with Spring AI, WebFlux |
| `chat-server/src/main/java/.../ChatServerApplication.java` | Boot main class |
| `chat-server/src/main/java/.../ChatController.java` | `POST /chat` endpoint, accepts `{ message, history[] }`, returns SSE stream |
| `chat-server/src/main/java/.../ChatService.java` | Orchestrates LLM calls with MCP tool definitions |
| `chat-server/src/main/java/.../mcp/AnimalRescueMcpTools.java` | MCP tool definitions: `getAvailableAnimals` |
| `chat-server/src/main/java/.../config/ChatServerConfig.java` | LLM client config, backend URL config |
| `chat-server/src/main/resources/application.yml` | Server port (8081), backend URL, LLM API config |
| `chat-server/src/test/java/.../ChatControllerTest.java` | Integration tests |
| `chat-server/src/test/java/.../mcp/AnimalRescueMcpToolsTest.java` | Tool execution unit tests |
| `frontend/src/components/chat-markdown.js` | Simple Markdown renderer for chat bubbles |
| `frontend/src/components/chat-markdown.css` | Styles for rendered markdown content |

## Modified Files

| File | Change |
|------|--------|
| `settings.gradle` | Add `include "chat-server"` |
| `frontend/src/App.js` | Replace `addChatMessage` placeholder logic with call to chat server; handle SSE streaming |
| `frontend/src/httpClient.js` | Add `sendChatMessage({ message, history })` function using `EventSource` or `fetch` with streaming |
| `frontend/src/components/chat-sidebar.js` | Show typing indicator during streaming; render messages with Markdown support |
| `frontend/src/components/chat-sidebar.css` | Styles for typing indicator and markdown content |
| `frontend/package.json` | Add `react-markdown` dependency for rendering |
| `scripts/local.sh` | Add chat-server start/stop to the `start` and `stop` commands |
| `e2e/cypress/e2e/rescue.cy.js` | Add tests for chat inquiry flow |

## Detailed Implementation Steps

### Step 1: Create the Chat Server Gradle Module

Add a new `chat-server/` directory as a sibling to `backend/` and `frontend/`.

**`chat-server/build.gradle`:**
```gradle
plugins {
    id 'org.springframework.boot' version '3.3.5'
    id 'io.spring.dependency-management' version '1.1.7'
    id 'java'
}

group = 'io.spring.cloud.samples.animalrescue'
version = '0.0.1-SNAPSHOT'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
    maven { url 'https://repo.spring.io/milestone' }
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-webflux'
    implementation 'org.springframework.ai:spring-ai-openai-spring-boot-starter'

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'io.projectreactor:reactor-test'
}
```

Update `settings.gradle` to include the new module:
```gradle
include "chat-server"
```

### Step 2: Implement MCP Tool Definitions

The MCP tools wrap the Animal Rescue backend REST API as callable tools for the LLM.

**`AnimalRescueMcpTools.java`:**

```java
@Component
public class AnimalRescueMcpTools {

    private final WebClient backendClient;

    public AnimalRescueMcpTools(@Value("${animal-rescue.backend-url}") String backendUrl) {
        this.backendClient = WebClient.builder().baseUrl(backendUrl).build();
    }

    @Tool(description = "Get all animals available for adoption at the rescue center. " +
          "Returns a list of animals with their name, description, rescue date, " +
          "avatar URL, and current adoption requests.")
    public Flux<Animal> getAvailableAnimals() {
        return backendClient.get()
            .uri("/animals")
            .retrieve()
            .bodyToFlux(Animal.class);
    }
}
```

The `Animal` record mirrors the backend's JSON response:
```java
public record Animal(
    Long id,
    String name,
    String rescueDate,
    String avatarUrl,
    String description,
    List<AdoptionRequest> adoptionRequests
) {}

public record AdoptionRequest(
    Long id,
    String adopterName,
    String email,
    String notes
) {}
```

### Step 3: Implement the Chat Service

The `ChatService` wires the LLM client with the MCP tools and manages the system prompt.

```java
@Service
public class ChatService {

    private final ChatClient chatClient;

    public ChatService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder
            .defaultSystem("""
                You are a friendly assistant for the Animal Rescue center.
                You help potential adopters find animals that match their preferences.
                When users ask about available animals, use the getAvailableAnimals tool
                to fetch current data. Present results in a friendly, readable format
                using Markdown. Include the animal's name, a brief description, and
                how many pending adoption requests they have. If no animals match
                the user's criteria, say so kindly and suggest broadening their search.
                """)
            .build();
    }

    public Flux<String> chat(String userMessage, List<ChatMessage> history) {
        return chatClient.prompt()
            .messages(buildMessages(history))
            .user(userMessage)
            .stream()
            .content();
    }
}
```

### Step 4: Implement the Chat Controller

**`ChatController.java`:**

```java
@RestController
@CrossOrigin(origins = "http://localhost:3000")
public class ChatController {

    private final ChatService chatService;

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestBody ChatRequest request) {
        return chatService.chat(request.message(), request.history());
    }
}

public record ChatRequest(
    String message,
    List<ChatMessage> history
) {}

public record ChatMessage(
    String role,    // "user" or "assistant"
    String content
) {}
```

The endpoint returns `text/event-stream` so the frontend can consume tokens as they arrive.

### Step 5: Configure the Chat Server

**`application.yml`:**
```yaml
server:
  port: 8081

animal-rescue:
  backend-url: http://localhost:8080

spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY:}
      chat:
        options:
          model: gpt-4o
          temperature: 0.7
```

The API key is read from an environment variable, never hardcoded.

### Step 6: Update the Frontend -- HTTP Client

Add a streaming chat function to `httpClient.js`:

```js
const chatServerBaseUrl = process.env.REACT_APP_CHAT_SERVER_URI || 'http://localhost:8081';

export function sendChatMessage({ message, history }) {
    return fetch(`${chatServerBaseUrl}/chat`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ message, history }),
    });
}
```

The response is an SSE stream. `App.js` will read it incrementally:

```js
async addChatMessage(text) {
    const userMsg = { id: Date.now(), text, sender: 'user', timestamp: new Date() };
    const assistantMsg = { id: Date.now() + 1, text: '', sender: 'assistant', timestamp: new Date() };

    this.setState(prev => ({
        chatMessages: [...prev.chatMessages, userMsg, assistantMsg],
    }));

    const history = this.state.chatMessages
        .filter(m => m.sender !== 'system')
        .map(m => ({ role: m.sender, content: m.text }));

    const response = await sendChatMessage({ message: text, history });
    const reader = response.body.getReader();
    const decoder = new TextDecoder();

    while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        const chunk = decoder.decode(value);
        this.setState(prev => {
            const messages = [...prev.chatMessages];
            const last = messages[messages.length - 1];
            messages[messages.length - 1] = { ...last, text: last.text + chunk };
            return { chatMessages: messages };
        });
    }
}
```

### Step 7: Add Markdown Rendering to Chat Sidebar

Install `react-markdown`:
```bash
cd frontend && npm install react-markdown
```

Create `chat-markdown.js` -- a thin wrapper:

```jsx
import ReactMarkdown from 'react-markdown';

export default function ChatMarkdown({ content }) {
    return (
        <div className="chat-markdown">
            <ReactMarkdown>{content}</ReactMarkdown>
        </div>
    );
}
```

Update `ChatSidebar` to use `ChatMarkdown` for assistant messages instead of plain text, and show a typing indicator (animated dots) while the assistant message text is still being streamed (i.e. the last message is from `assistant` and is empty or still growing).

### Step 8: Update `scripts/local.sh`

Add chat-server lifecycle management:

```bash
stopChatServer() {
  if lsof -i:8081 -t &> /dev/null; then
    printf "\n======== Stopping chat-server ========\n"
    kill $(lsof -i:8081 -t) || true
  fi
}

startChatServer() {
  stopChatServer
  printf "\n======== Starting chat-server ========\n"
  if [[ $1 == "$QUIET_MODE" ]]; then
    ./gradlew :chat-server:bootRun > "$ROOT_DIR/scripts/out/chat_server_output.log" &
  else
    ./gradlew :chat-server:bootRun &
  fi
}
```

Update `start()` to call `startChatServer` after `startBackend` (the chat server needs the backend running). Update `stop()` to call `stopChatServer`.

### Step 9: Write Tests

**Chat Server -- Integration Test (`ChatControllerTest.java`):**
- Mock the LLM client to return a canned response
- Verify `POST /chat` returns an SSE stream
- Verify the MCP tool is invoked when the LLM requests it

**Chat Server -- MCP Tool Test (`AnimalRescueMcpToolsTest.java`):**
- Use `MockWebServer` to stub `GET /animals`
- Verify `getAvailableAnimals()` correctly deserializes the response

**E2E (Cypress):**
```js
describe('natural language animal inquiry', () => {
    before(() => {
        cy.visit('/');
    });

    it('opens chat and asks about animals', () => {
        cy.get('.chat-fab').click();
        cy.get('.chat-sidebar-input input').type('Are there any lazy cats?{enter}');
        // Wait for streamed response
        cy.get('.chat-sidebar-messages .chat-markdown', { timeout: 15000 })
            .should('exist');
    });

    it('displays animal information in the response', () => {
        cy.get('.chat-sidebar-messages')
            .should('contain.text', 'Chocobo')  // "chubby, lazy" cat
            .or('contain.text', 'Mittens');      // "chubby cat...lounging"
    });
});
```

Note: E2E tests for LLM responses are inherently non-deterministic. The tests verify structural correctness (a response appears, it contains animal names) rather than exact wording.

### Step 10: Verify Full Test Suite

```bash
./scripts/local.sh start --quiet
./scripts/local.sh e2e --quiet
./scripts/local.sh stop
./gradlew :chat-server:test
./gradlew :backend:test
```

## How Context-Aware Filtering Works

The acceptance criteria require the LLM to filter results based on queries like "Show me small dogs" or "Are there any animals with high energy levels?" The Animal Rescue schema has no explicit `species`, `breed`, or `energyLevel` fields. The approach:

1. The `getAvailableAnimals` MCP tool returns **all** animals with their full `description` and `avatarUrl`
2. The LLM's system prompt instructs it to interpret descriptions semantically (e.g. "chubby, lazy" = low energy; "lots of energy and loves the outdoors" = high energy)
3. The `avatarUrl` contains breed hints in the path (e.g. `/breeds/pug/`, `rhodesian-ridgeback`, `pomeranian`) which the LLM can use to infer species and size
4. The LLM filters in its reasoning and only presents matching animals in its response

This is a pragmatic approach that avoids schema changes to the existing backend. If more structured filtering is needed in the future, a `species` and `breed` column can be added to the `animal` table.

## Acceptance Criteria Mapping

| Criteria | How It's Met |
|----------|-------------|
| Chat window establishes persistent connection to Chat/MCP Server | Frontend sends `POST /chat` to the chat server (port 8081) and reads the SSE response stream. Connection is per-message; conversation history is sent with each request. |
| MCP Integration: Chat Server calls `GET /animals` | `AnimalRescueMcpTools.getAvailableAnimals()` is registered as an MCP tool. The LLM invokes it when the user asks about animals. The tool calls `GET /animals` on the backend (port 8080). |
| Context Awareness: LLM filters based on user queries | The LLM receives the full animal list from the tool and uses its language understanding to match descriptions to user criteria. System prompt guides this behavior. |
| Response Rendering: Markdown or structured responses | `react-markdown` renders assistant messages. The system prompt instructs the LLM to format responses with Markdown (bold names, bullet points, etc.). |

## Risks and Mitigations

| Risk | Mitigation |
|------|-----------|
| LLM API key management | Key is read from `OPENAI_API_KEY` env var; never committed. Document in README. For local dev, use `.envrc` (already gitignored via direnv). |
| LLM response latency | SSE streaming shows tokens as they arrive, so the user sees progress immediately. Add a typing indicator in the sidebar. |
| LLM hallucination (inventing animals) | System prompt explicitly says "only mention animals returned by the tool." The tool returns real data, so the LLM has ground truth to work with. |
| Non-deterministic E2E tests | Tests assert structural properties (response exists, contains known animal names) rather than exact text. |
| No species/breed fields in schema | LLM infers from description text and avatar URL paths. Document this as a known limitation; suggest schema enhancement as a follow-up. |
| Cost of LLM API calls | Rate limiting on the chat endpoint (e.g. 10 req/min per session). For demos, a smaller/cheaper model can be configured via `application.yml`. |
| Chat server adds operational complexity | It's a standard Spring Boot app using the same build system (Gradle) and deployment patterns. `local.sh` manages its lifecycle alongside the other services. |

## Estimated Effort

| Task | Estimate |
|------|----------|
| Step 1: Chat server Gradle module setup | 30 min |
| Step 2: MCP tool definitions | 30 min |
| Step 3: Chat service + LLM integration | 1 hour |
| Step 4: Chat controller (SSE endpoint) | 30 min |
| Step 5: Configuration (application.yml, env vars) | 15 min |
| Step 6: Frontend HTTP client + streaming | 45 min |
| Step 7: Markdown rendering in sidebar | 30 min |
| Step 8: local.sh updates | 15 min |
| Step 9: Tests (server + e2e) | 1 hour |
| Step 10: Full integration test run | 30 min |
| **Total** | **~5.5 hours** |
