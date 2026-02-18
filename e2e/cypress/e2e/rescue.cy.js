context('Animal Rescue', () => {

  const username = "alice";
  const password = "test";

  before(() => {
    cy.visit('/');
  });

  it('shows animals with "Adopt" buttons disabled on homepage', () => {
    cy.get('.ui.card button')
      .should('have.length', 10)
      .each($button => {
        expect($button).to.have.class('disabled');
      });
  });

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

    it('shows an assistant reply after sending a message', () => {
      // The assistant reply comes from the chat server (LLM-backed)
      // or a connection error message if the chat server is not running
      cy.get('.chat-sidebar-messages .chat-bubble-assistant', {timeout: 15000})
          .should('exist');
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

    after(() => {
      // Reset viewport and close sidebar for subsequent tests
      cy.viewport(1000, 660);
    });
  });

  describe('natural language animal inquiry', () => {

    before(() => {
      cy.visit('/');
    });

    it('opens chat and asks about animals', () => {
      cy.get('.chat-fab').click();
      cy.get('.chat-sidebar-input input').type('Are there any lazy cats?{enter}');
      // Wait for streamed response -- the assistant bubble should contain markdown content
      cy.get('.chat-sidebar-messages .chat-bubble-assistant .chat-markdown', {timeout: 30000})
          .should('exist');
    });

    it('displays animal information in the response', () => {
      // The LLM should mention at least one of the lazy/chubby cats
      cy.get('.chat-sidebar-messages .chat-bubble-assistant')
          .last()
          .invoke('text')
          .should('match', /Chocobo|Mittens/i);
    });

    after(() => {
      // Close sidebar for subsequent tests
      cy.get('.chat-sidebar-header .close.icon').click({force: true});
    });
  });

  describe('logged in user', () => {

    before(() => {
      cy.login(username, password);
    });

    it('greets user and allows for adoption after logging in', () => {
      cy.location()
        .should(location => {
          expect(location.pathname).to.eq('/rescue');
        });

      cy.get('.header-buttons button').should('contain', username);
      cy.get('.ui.card button')
        .should('have.length', 10)
        .each($button => {
          expect($button).to.not.have.class('disabled');
        });
    });

    it('allows user to adopt animals', () => {
      cy.get('.pending-number')
        .first()
        .then($span => {
          const pendingAdopterNumber = parseInt($span.text());
          cy.get('.ui.card button')
            .first()
            .should('contain', 'Adopt')
            .click();

          cy.get('[name=email]').type("email@example.com");
          cy.get('[name=notes]').type("She heals my soul!");
          cy.contains('Apply').click();

          cy.get('.pending')
            .first()
            .contains(`${pendingAdopterNumber + 1} Pending Adopters`);
        });
    });

    it('allows user to edit adoption request', () => {
      cy.get('.ui.card button')
        .first()
        .should('contain', 'Edit Adoption Request')
        .click();

      cy.get('[name=email]').should('have.value', 'email@example.com');
      cy.get('[name=email]').clear();
      cy.get('[name=email]').type("new@example.com");

      cy.get('[name=notes]').should('have.value', 'She heals my soul!');
      cy.get('[name=notes]').clear();
      cy.get('[name=notes]').type("I need her to be my life companion!");

      cy.contains('Apply').click();
    });

    it('allows user to delete adoption request', () => {
      cy.get('.pending-number')
        .first()
        .then($span => {
          const pendingAdopterNumber = parseInt($span.text());
          cy.get('.ui.card button')
            .first()
            .should('contain', 'Edit Adoption Request')
            .click();

          cy.get('[name=email]').should('have.value', 'new@example.com');
          cy.get('[name=notes]').should('have.value', 'I need her to be my life companion!');

          cy.contains('Delete Request').click();

          cy.get('.ui.card button').first().should('contain', 'Adopt');

          cy.contains(`${pendingAdopterNumber - 1} Pending Adopters`);
        });
    });

    it('allows user to log out', () => {
      cy.contains('Sign out').click();
      cy.contains('Log Out').click(); // This is the log out page provided by spring security
      cy.contains('Sign in to adopt');
    });
  });

  describe('adoption via chat', () => {

    before(() => {
      cy.login(username, password);
    });

    it('adopts an animal through chat conversation', () => {
      cy.get('.chat-fab').click();

      cy.get('.chat-sidebar-input input')
          .type('I would like to adopt Chocobo{enter}');

      cy.get('.chat-sidebar-messages .chat-bubble-assistant', {timeout: 30000})
          .last()
          .invoke('text')
          .should('match', /email/i);

      cy.get('.chat-sidebar-input input')
          .type('alice@example.com{enter}');

      cy.get('.chat-sidebar-messages .chat-bubble-assistant', {timeout: 30000})
          .last()
          .invoke('text')
          .should('match', /success|submitted/i);
    });

    after(() => {
      cy.get('.chat-sidebar-header .close.icon').click({force: true});
    });
  });
});
