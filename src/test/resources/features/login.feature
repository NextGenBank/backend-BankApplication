Feature: User login

  Scenario: Successful login with correct credentials
    Given I successfully log in with email "alice@example.com" and password "alice123"
    Then the response status should be 200

  Scenario: Successful login with correct credentials returns JWT
    Given I successfully log in with email "alice@example.com" and password "alice123"
    Then the response status should be 200
    And a JWT token should be returned

  Scenario: Failed login with incorrect password
    Given I attempt to log in with email "alice@example.com" and password "wrongpassword"
    Then the response status should be 401

  Scenario: Failed login with non-existent email
    Given I attempt to log in with email "notfound@example.com" and password "anyPassword"
    Then the response status should be 401
