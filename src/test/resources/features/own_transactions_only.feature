Feature: Customer Transaction Privacy

  Scenario: Customer should only see their own transactions
    Given a registered customer with email "alice@example.com" and password "alice123"
    And the customer logs in with email "alice@example.com" and password "alice123"
    When the customer requests their transaction history
    Then the response should contain only transactions belonging to "Alice Smith"

