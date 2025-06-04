Feature: Login failure scenarios

  Scenario: Login fails with missing email
    Given I attempt to log in with email "" and password "alice123"
    Then the response status should be 400

  Scenario: Login fails with missing password
    Given I attempt to log in with email "alice@example.com" and password ""
    Then the response status should be 400

  Scenario: Login fails with invalid email format
    Given I attempt to log in with email "not-an-email" and password "somepass"
    Then the response status should be 400
