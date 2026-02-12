# Implementation Plan: Contextual Sidebar Integration

## User Story

> As a user navigating the application, I want to have a persistent, minimizable chat sidebar, so that I can ask questions while browsing different sections of the rescue site.

## Current State Analysis

The frontend is a React 18 class-component application using Semantic UI React. It is a single-page app with no client-side routing -- `App.js` renders a header, carousel, and `AnimalCards` grid in a single view. State is managed locally in each component (no Redux, no global store beyond a minimal `AppContext` that provides a `refresh` callback). The app proxies API calls to a Spring Boot WebFlux backend on port 8080.

Key constraints:
- All existing components are **class components** (not hooks-based)
- The only shared context (`AppContext`) carries a single `refresh` function
- There is no client-side router today; the acceptance criteria mention navigating between "Available Animals" and "My Adoptions" pages, which **do not yet exist** as separate views
- Semantic UI React is the design system in use
- The e2e test suite (Cypress) selects elements by CSS class (`.ui.card button`, `.header-buttons button`, etc.)

## Scope Decisions

### In Scope
1. A new **ChatSidebar** component with open/minimized/closed states
2. A **floating action button (FAB)** to toggle the sidebar
3. Chat message history that persists across the component lifecycle (in-memory state held at the `App` level)
4. Responsive layout: sidebar overlays on mobile, sits beside content on desktop
5. A simple local chat UI (message list + input) -- the backend integration for an actual AI/chat service is **out of scope** for this story; messages will be stored client-side only

### Out of Scope (future stories)
- Backend chat API or AI integration
- "My Adoptions" page / client-side routing (referenced in acceptance criteria but is a separate feature)
- Chat persistence across page reloads (localStorage or server-side storage)

## Architecture

```
App.js
 |- <header />
 |- <Carousel />
 |- <AppContext.Provider>
 |    \- <AnimalCards />
 |- <ChatSidebar />          <-- NEW (rendered outside main content flow)
 \- <ChatFab />              <-- NEW (fixed-position toggle button, rendered inside ChatSidebar)
```

Chat state will be **lifted into `App.js`** so it survives any future re-renders or child unmounts. When client-side routing is added later, `App.js` will remain the outer shell, so the sidebar state will naturally persist across route changes.

### New Files

| File | Purpose |
|------|---------|
| `frontend/src/components/chat-sidebar.js` | Sidebar panel: header, message list, input form, and FAB toggle |
| `frontend/src/components/chat-sidebar.css` | Sidebar and FAB styles, responsive breakpoints |

### Modified Files

| File | Change |
|------|--------|
| `frontend/src/App.js` | Add chat state (`messages`, `isSidebarOpen`), render `ChatSidebar`, pass props |
| `frontend/src/App.css` | Add `.App-body` transition for sidebar push on desktop; ensure no z-index conflicts |
| `e2e/cypress/e2e/rescue.cy.js` | Add test block for sidebar open/close and message persistence |

## Detailed Implementation Steps

### Step 1: Add Chat State to `App.js`

Add two new state fields to the `App` constructor:

```js
this.state = {
    // ...existing fields
    chatMessages: [],       // Array of { id, text, sender, timestamp }
    isSidebarOpen: false,
};
```

Add handler methods:

- `toggleSidebar()` -- flips `isSidebarOpen`
- `addChatMessage(text)` -- appends a message from the user, then appends a placeholder bot reply (e.g. "Thanks for your question! A volunteer will get back to you soon.")

These are defined on `App` so the message array is never lost when child components unmount.

### Step 2: Create `ChatSidebar` Component

A class component receiving props:

| Prop | Type | Description |
|------|------|-------------|
| `isOpen` | `bool` | Controls slide-in/slide-out |
| `messages` | `array` | Chat history |
| `onSendMessage` | `func` | Callback to `App.addChatMessage` |
| `onClose` | `func` | Callback to `App.toggleSidebar` |

Internal structure:

```
<div className="chat-sidebar {open|closed}">
  <div className="chat-sidebar-header">
    <span>Chat with us</span>
    <Icon name="close" onClick={onClose} />
  </div>
  <div className="chat-sidebar-messages">
    {messages.map(m => <ChatBubble />)}
    <div ref={bottomRef} />          <-- auto-scroll anchor
  </div>
  <div className="chat-sidebar-input">
    <Form.Input ... />
    <Button icon="send" />
  </div>
</div>
```

Uses Semantic UI `Icon`, `Button`, and `Form.Input` to stay consistent with the existing design language.

### Step 3: Create the Floating Action Button (FAB)

Rendered inside `ChatSidebar` -- a circular button fixed to the bottom-right corner:

```jsx
{!isOpen && (
  <button className="chat-fab" onClick={onToggle} aria-label="Open chat">
    <Icon name="comment" />
  </button>
)}
```

The FAB is hidden when the sidebar is open (the sidebar header has its own close button).

### Step 4: Styles (`chat-sidebar.css`)

**Desktop (min-width: 601px):**
- Sidebar: `position: fixed; right: 0; top: 0; height: 100vh; width: 360px; z-index: 1000`
- Slide-in via `transform: translateX(0)` / slide-out via `transform: translateX(100%)`
- `transition: transform 0.3s ease`
- Main `.App-body` gets `margin-right: 360px` when sidebar is open (smooth transition) so cards reflow and are not hidden

**Mobile (max-width: 600px):**
- Sidebar: `position: fixed; bottom: 0; left: 0; width: 100%; height: 60vh; z-index: 1000`
- Slides up from bottom (`translateY(0)` open / `translateY(100%)` closed)
- A semi-transparent backdrop overlay behind the sidebar; tapping it closes the sidebar
- The animal cards remain fully accessible when the sidebar is closed
- The FAB sits at `bottom: 20px; right: 20px`

**FAB:**
- `position: fixed; bottom: 20px; right: 20px; z-index: 999`
- `width: 56px; height: 56px; border-radius: 50%`
- Background color: `#67B547` (matches the existing green theme)
- Box shadow for elevation
- Subtle scale animation on hover

### Step 5: Wire into `App.js` Render

```jsx
render() {
    return (
        <div className={`App ${this.state.isSidebarOpen ? 'sidebar-open' : ''}`}>
            <header>...</header>
            <Carousel />
            <div className="App-body">
                <AppContext.Provider value={{refresh: () => this.fetchAnimals()}}>
                    <AnimalCards ... />
                </AppContext.Provider>
            </div>
            <ChatSidebar
                isOpen={this.state.isSidebarOpen}
                messages={this.state.chatMessages}
                onSendMessage={(text) => this.addChatMessage(text)}
                onClose={() => this.toggleSidebar()}
                onOpen={() => this.toggleSidebar()}
            />
        </div>
    );
}
```

The `sidebar-open` class on the root div drives the desktop margin transition in CSS.

### Step 6: Update E2E Tests

Add a new `describe` block in `rescue.cy.js`:

```js
describe('chat sidebar', () => {
    it('shows FAB on page load', () => {
        cy.get('.chat-fab').should('be.visible');
    });

    it('opens sidebar when FAB is clicked', () => {
        cy.get('.chat-fab').click();
        cy.get('.chat-sidebar').should('have.class', 'open');
    });

    it('sends a message and displays it', () => {
        cy.get('.chat-sidebar-input input').type('Hello{enter}');
        cy.get('.chat-sidebar-messages').should('contain', 'Hello');
    });

    it('closes sidebar and preserves messages', () => {
        cy.get('.chat-sidebar-header .close.icon').click();
        cy.get('.chat-sidebar').should('not.have.class', 'open');
        cy.get('.chat-fab').click();
        cy.get('.chat-sidebar-messages').should('contain', 'Hello');
    });

    it('does not block animal cards on mobile', () => {
        cy.viewport(375, 667);
        cy.get('.chat-sidebar-header .close.icon').click();
        cy.get('.ui.card').first().should('be.visible');
    });
});
```

### Step 7: Verify Existing Tests Still Pass

The new components are additive. The FAB and sidebar are outside the `.ui.card` and `.header-buttons` selectors used by existing Cypress tests, so there should be no interference. Verify by running the full suite:

```bash
./scripts/local.sh start --quiet
./scripts/local.sh e2e --quiet
./scripts/local.sh stop
```

## Acceptance Criteria Mapping

| Criteria | How It's Met |
|----------|-------------|
| FAB or sidebar toggle on main dashboard | Fixed-position FAB in bottom-right corner, always visible when sidebar is closed |
| Chat history persists across navigation | State held in `App.js` (top-level component); survives child re-renders. When routing is added later, `App` remains the shell. |
| Chat window must not block animal cards on mobile | Mobile: sidebar is a bottom sheet (60vh) that slides away; FAB is small and positioned in the corner. Backdrop click closes the sidebar. Cards are fully scrollable when sidebar is closed. |

## Risks and Mitigations

| Risk | Mitigation |
|------|-----------|
| No client-side router exists yet; "navigate between pages" can't be fully tested | State is in `App.js` which is the permanent outer shell. When routing is added, sidebar will persist by design. Add a note in the PR for future verification. |
| Semantic UI's `Modal` (used by adoption requests) may have z-index conflicts with the sidebar | Sidebar uses `z-index: 1000`; Semantic UI modals use `z-index: 1001` by default. Modals will correctly appear above the sidebar. |
| Class component patterns are verbose for state management | Keep it consistent with the existing codebase. A future refactor to hooks/context can simplify this. |
| No real chat backend | The placeholder auto-reply makes the UI feel functional for demo purposes. The component API (`onSendMessage` callback) is designed so a real backend can be wired in later without changing the component tree. |

## Estimated Effort

| Task | Estimate |
|------|----------|
| Step 1: App.js state changes | 15 min |
| Step 2: ChatSidebar component | 45 min |
| Step 3: FAB | 15 min |
| Step 4: CSS / responsive styles | 30 min |
| Step 5: Wiring + App.css tweaks | 15 min |
| Step 6: E2E tests | 20 min |
| Step 7: Full test run + fixes | 15 min |
| **Total** | **~2.5 hours** |
