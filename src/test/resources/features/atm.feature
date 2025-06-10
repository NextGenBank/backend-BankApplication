Feature: ATM Functionality

  Scenario: Successful deposit
    Given a user "John Doe" with IBAN "NL123456789" and balance 100.0
    And I am authenticated as "John Doe"
    When I POST to "/api/atm/deposit" with body {"toIban": "NL123456789", "amount": 50.0}
    Then the response status code should be 200
    And the JSON field "transactionType" should be "DEPOSIT"
    And the JSON field "amount" should be "50.0"
    And the JSON field "toIban" should be "NL123456789"
    And the account balance for IBAN "NL123456789" should be 150.0
    And there should be a transaction with type "DEPOSIT", amount 50.0, fromIban "null", toIban "NL123456789"

  Scenario: Deposit with invalid toIban
    Given a user "John Doe" with IBAN "NL123456789" and balance 100.0
    And I am authenticated as "John Doe"
    When I POST to "/api/atm/deposit" with body {"toIban": "INVALID_IBAN", "amount": 50.0}
    Then the response status code should be 400
    And the response body should contain "Account not found"

  Scenario: Successful withdrawal
    Given a user "John Doe" with IBAN "NL123456789" and balance 100.0
    And I am authenticated as "John Doe"
    When I POST to "/api/atm/withdraw" with body {"fromIban": "NL123456789", "amount": 30.0}
    Then the response status code should be 200
    And the JSON field "transactionType" should be "WITHDRAWAL"
    And the JSON field "amount" should be "30.0"
    And the JSON field "fromIban" should be "NL123456789"
    And the account balance for IBAN "NL123456789" should be 70.0
    And there should be a transaction with type "WITHDRAWAL", amount 30.0, fromIban "NL123456789", toIban "null"

  Scenario: Withdrawal with insufficient funds
    Given a user "John Doe" with IBAN "NL123456789" and balance 20.0
    And I am authenticated as "John Doe"
    When I POST to "/api/atm/withdraw" with body {"fromIban": "NL123456789", "amount": 30.0}
    Then the response status code should be 400
    And the response body should contain "Insufficient funds"