Feature: Filter customer transactions

  Scenario: Filter transactions by IBAN
    Given a registered customer with email "alice@example.com" and password "alice123"
    And the customer logs in with email "alice@example.com" and password "alice123"
    When the customer filters transactions by IBAN "NL12345678901234567890"
    Then the response should contain only transactions involving IBAN "NL12345678901234567890"

  Scenario: Filter transactions by name
    Given a registered customer with email "alice@example.com" and password "alice123"
    And the customer logs in with email "alice@example.com" and password "alice123"
    When the customer filters transactions by name "Charlie"
    Then the response should contain only transactions involving name "Charlie"

  Scenario: Filter transactions by type
    Given a registered customer with email "alice@example.com" and password "alice123"
    And the customer logs in with email "alice@example.com" and password "alice123"
    When the customer filters transactions by type "INTERNAL"
    Then the response should contain only transactions of type "INTERNAL"
