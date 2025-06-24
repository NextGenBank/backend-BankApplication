Feature: Employee can configure customer transfer limits

  Scenario: Employee can view account transfer limits
    Given an employee with email "bob@example.com" and password "bob123"
    # Alice from DataInitializer with ID 1
    When the employee logs in
    And the employee requests accounts for customer with id 1
    Then each account should have a configurable transfer limit

  Scenario: Employee can update account transfer limit
    Given an employee with email "bob@example.com" and password "bob123"
    # Alice's checking account from DataInitializer
    When the employee logs in
    And the employee updates the transfer limit for IBAN "NL12345678901234567890" to 7500.00
    Then the account with IBAN "NL12345678901234567890" should have a transfer limit of 7500.00

  Scenario: Employee cannot set negative transfer limit
    Given an employee with email "bob@example.com" and password "bob123"
    # Charlie's checking account from DataInitializer
    When the employee logs in
    And the employee attempts to update the transfer limit for IBAN "NL22223333444455556666" to -1000.00
    Then the system should reject the negative transfer limit
    And the account transfer limit should remain unchanged