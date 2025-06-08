Feature: Transaction Sorting

  Scenario: Transactions are returned sorted by most recent first
    Given the customer logs in for sorting tests with email "alice@example.com" and password "alice123"
    When the customer requests transactions sorted by most recent
    Then the transactions should be sorted from newest to oldest

  Scenario: Customer can choose to sort by amount
    Given the customer logs in for sorting tests with email "alice@example.com" and password "alice123"
    When the customer requests transactions sorted by amount
    Then the transactions should be sorted from highest to lowest amount

  Scenario: Customer can choose to sort by type
    Given the customer logs in for sorting tests with email "alice@example.com" and password "alice123"
    When the customer requests transactions sorted by type
    Then the transactions should be sorted alphabetically by type
