Feature: No Transactions

  Scenario: Customer has no transactions
    Given a registered customer with email "newuser@example.com" and password "newpass123"
    And the customer logs in with email "newuser@example.com" and password "newpass123"
    When the customer requests their transaction history
    Then the response should contain 0 transactions
