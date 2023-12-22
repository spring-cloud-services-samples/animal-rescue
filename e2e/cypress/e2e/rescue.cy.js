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
});
