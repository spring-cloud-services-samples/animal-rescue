context('Animal Rescue', () => {

  const username = "mysterious_adopter";
  const password = "test";

  before(() => {
    cy.visit('http://localhost:3000');
  });

  beforeEach(() => {
    Cypress.Cookies.preserveOnce('Current-User', 'SESSION', 'X-Uaa-Csrf', 'JSESSIONID');
  });

  it('shows animals with "Adopt" buttons disabled on homepage', () => {
    cy.get('.ui.card button')
      .should('have.length', 10)
      .each($button => {
        expect($button).to.have.class('disabled');
      });
  });

  it('greets user and allows for adoption after logging in', () => {
    cy.contains('Sign in to adopt').click();

    optionallyLogIn(username, password);

    cy.location()
      .should(location => {
        expect(location.pathname).to.eq('/rescue/admin');
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

        cy.wait(500);
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

    cy.get('[name=email]')
      .should('have.value', 'email@example.com')
      .clear()
      .type("new@example.com");
    cy.get('[name=notes]')
      .should('have.value', 'She heals my soul!')
      .clear()
      .type("I need her to be my life companion!");

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

        cy.get('.ui.card button').first().should('contain', 'Adopt')

        cy.contains(`${pendingAdopterNumber - 1} Pending Adopters`);
      });
  });

  const optionallyLogIn = (username, password) => {
    cy.location().then(location => {
      if (location.pathname !== '/rescue/admin') { // If cookie exists in browser then we can skip the log in step
        cy.get('input[name=username]').type(username);
        cy.get('input[name=password]').type(`${password}{enter}`); // {enter} causes the form to submit
      }

      cy.location().then(location => {
        if (location.pathname !== '/rescue/admin') {
          cy.get('#authorize').click() // authorizes scopes
        }
      });
    });
  };
});
