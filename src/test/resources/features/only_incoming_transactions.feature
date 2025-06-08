Feature: Transaction History

  Scenario: Customer has only incoming transactions
    Given a registered customer with email "dana@example.com" and password "dana123"
    And the customer logs in with email "dana@example.com" and password "dana123"
    When the customer requests their transaction history
    Then the response should contain 1 transactions
