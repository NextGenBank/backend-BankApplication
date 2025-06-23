Feature: Transaction Sorting

  Scenario: Customer can choose to sort by type
    Given the customer logs in for sorting tests with email "alice@example.com" and password "alice123"
    When the customer requests transactions sorted by type
    Then the transactions should be sorted alphabetically by type
