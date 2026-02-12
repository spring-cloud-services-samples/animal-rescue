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
- The backend is a Spring Boot 3.5.10 WebFlux app running on port 8080
- The backend already includes `spring-ai-starter-mcp-server` (Spring AI 1.1.2 BOM) -- it is configured as an **MCP server**, meaning the chat server can connect to it as an MCP client and invoke tools directly via the MCP protocol rather than wrapping raw REST calls

**Infrastructure**:
- No chat server exists yet, but the backend is already an MCP server
- Spring AI 1.1.2 BOM is already declared in the backend's `build.gradle`

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

The **Chat Server** is a new Spring Boot 3.5.10 application that:
1. Receives user messages from the frontend
2. Sends them to an LLM (e.g. OpenAI, Anthropic, or a local model) with tool definitions
3. The LLM decides when to call the `getAvailableAnimals` tool
4. The chat server, acting as an **MCP client**, invokes the tool on the Animal Rescue backend (which is already an MCP server via `spring-ai-starter-mcp-server`)
5. The LLM uses the returned animal data to compose a natural-language response
6. The response is streamed back to the frontend

### Why a Separate Chat Server?

- **Separation of concerns**: The Animal Rescue backend is a domain API and MCP server; the chat server is the LLM orchestration layer and MCP client
- **LLM API keys stay server-side**: Never exposed to the browser
- **MCP protocol**: The backend already exposes tools via `spring-ai-starter-mcp-server`. The chat server uses `spring-ai-starter-mcp-client` to connect to it natively -- no manual REST wrapping needed
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
| `chat-server/build.gradle` | Spring Boot 3.5.10 app with Spring AI 1.1.2, WebFlux, MCP client |
| `chat-server/src/main/java/.../ChatServerApplication.java` | Boot main class |
| `chat-server/src/main/java/.../ChatController.java` | `POST /chat` endpoint, accepts `{ message, history[] }`, returns SSE stream |
| `chat-server/src/main/java/.../ChatService.java` | Orchestrates LLM calls; the MCP client auto-discovers tools from the backend MCP server |
| `chat-server/src/main/java/.../config/ChatServerConfig.java` | LLM client config, MCP client config |
| `chat-server/src/main/resources/application.yml` | Server port (8081), backend URL, LLM API config |
| `chat-server/src/test/java/.../ChatControllerTest.java` | Integration tests |
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
    id 'org.springframework.boot' version '3.5.10'
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
}

dependencyManagement {
    imports {
        mavenBom "org.springframework.ai:spring-ai-bom:1.1.2"
    }
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-webflux'
    implementation 'org.springframework.ai:spring-ai-starter-model-openai'
    implementation 'org.springframework.ai:spring-ai-starter-mcp-client'

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'io.projectreactor:reactor-test'
}
```

Note: The Spring AI BOM 1.1.2 is used for dependency management, matching the backend. The artifact names follow the new Spring AI 1.x convention (`spring-ai-starter-model-openai` instead of the old `spring-ai-openai-spring-boot-starter`). The `spring-ai-starter-mcp-client` dependency enables the chat server to connect to the backend's MCP server and auto-discover its tools. No milestone repository is needed -- Spring AI 1.1.2 is GA and available from Maven Central.

Update `settings.gradle` to include the new module:
```gradle
include "chat-server"
```

### Step 2: MCP Tool Discovery (Backend is Already an MCP Server)

The backend already includes `spring-ai-starter-mcp-server` and exposes tools via the MCP protocol. The chat server uses `spring-ai-starter-mcp-client` to **auto-discover** these tools at startup -- no manual tool definitions or REST wrappers are needed in the chat server.

The backend's MCP server should expose a `getAvailableAnimals` tool (defined in the backend codebase using `@Tool`). If this tool does not yet exist in the backend, it needs to be added:

**Backend -- `AnimalRescueMcpTools.java` (if not already present):**

```java
@Component
public class AnimalRescueMcpTools {

    private final AnimalRepository animalRepository;

    public AnimalRescueMcpTools(AnimalRepository animalRepository) {
        this.animalRepository = animalRepository;
    }

    @Tool(description = "Get all animals available for adoption at the rescue center. " +
          "Returns a list of animals with their name, description, rescue date, " +
          "avatar URL, and current adoption requests.")
    public Flux<Animal> getAvailableAnimals() {
        return animalRepository.findAll();
    }
}
```

The chat server's MCP client configuration (in `application.yml`) points to the backend's MCP endpoint:

```yaml
spring:
  ai:
    mcp:
      client:
        stdio:
          servers:
            animal-rescue:
              command: ./gradlew
              args: [":backend:bootRun"]
```

Or, if the backend exposes MCP over HTTP (SSE transport), the client connects via URL. The exact transport depends on how the backend's `spring-ai-starter-mcp-server` is configured. The key point is that **tools are discovered automatically** -- the chat server does not need to define `AnimalRescueMcpTools` locally.

### Step 3: Implement the Chat Service

The `ChatService` wires the LLM client with the MCP-discovered tools and manages the system prompt. Spring AI 1.1.2's `ChatClient.Builder` is auto-configured with the MCP client's tool definitions, so tools discovered from the backend MCP server are automatically available to the LLM.

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

The MCP tools (e.g. `getAvailableAnimals`) are auto-registered from the backend MCP server -- no explicit tool registration is needed in the chat server code.

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

spring:
  ai:
    openai:
      api-key: ${OPENAI_API_KEY:}
      chat:
        options:
          model: gpt-4o
          temperature: 0.7
    mcp:
      client:
        stdio:
          servers:
            animal-rescue-backend:
              command: ./gradlew
              args: [":backend:bootRun"]
```

The API key is read from an environment variable, never hardcoded. The MCP client configuration tells the chat server how to connect to the backend's MCP server. The exact transport (stdio vs. HTTP/SSE) depends on how the backend's `spring-ai-starter-mcp-server` is configured -- adjust accordingly.

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

Note: MCP tool definitions live in the backend (not the chat server), so tool-level unit tests belong in the backend's test suite. The chat server tests focus on the LLM orchestration and SSE streaming.

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
| MCP Integration: Chat Server calls `GET /animals` | The backend exposes `getAvailableAnimals` as an MCP tool via `spring-ai-starter-mcp-server`. The chat server's MCP client (`spring-ai-starter-mcp-client`) auto-discovers this tool. The LLM invokes it when the user asks about animals. |
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
