context('Animal Rescue', () => {

  const username = "alice";
  const password = "test";

  before(() => {
    cy.visit('http://localhost:3000');
  });

  beforeEach(() => {
    Cypress.Cookies.preserveOnce('Current-User', 'SESSION', 'X-Uaa-Csrf', 'JSESSIONID');
  });

  it('shows animals with "Adopt" buttons disabled on homepage', () => {
    cy.get('.ui.card button')
      .should('have.length', 9)
      .each($button => {
        expect($button).to.have.class('disabled');
      });
  });

  it('greets user and allows for adoption after logging in', () => {
    cy.contains('Sign in to adopt').click({force: true});

    logIn(username, password);

    cy.location()
      .should(location => {
        expect(location.pathname).to.eq('/rescue');
      });

    cy.get('.header-buttons button').should('contain', username);
    cy.get('.ui.card button')
      .should('have.length', 9)
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

        cy.get('.ui.card button').first().should('contain', 'Adopt');

        cy.contains(`${pendingAdopterNumber - 1} Pending Adopters`);
      });
  });

  it('allows user to log out', () => {
    cy.contains('Sign out').click();
    cy.contains('Log Out').click(); // This is the log out page provided by spring security
    cy.contains('Sign in to adopt');
  });

  const logIn = (username, password) => {
    cy.get('input[name=username]').type(username);
    cy.get('input[name=password]').type(`${password}{enter}`); // {enter} causes the form to submit
  };
});
