Feature: Login failure scenarios

  Scenario: Login fails with incorrect password
    Given I attempt to log in with email "alice@example.com" and password "wrongpassword"
    Then the response status should be 401
