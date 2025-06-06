Feature: Transaction History Access Control

  Scenario: Customer tries to view transactions without logging in
    When the customer tries to request transactions without a token
    Then the response should be unauthorized
