Feature: Employee transfer operations

  Scenario: Employee can successfully transfer funds between customer accounts
    Given an employee with email "bob@example.com" and password "bob123"
    And a customer with source account "NL12345678901234567890" and balance 1000.00
    And a customer with destination account "NL22223333444455556666" and balance 2000.00
    When the employee logs in
    And the employee transfers 200.00 from "NL12345678901234567890" to "NL22223333444455556666"
    Then the source account "NL12345678901234567890" should have balance 800.00
    And the destination account "NL22223333444455556666" should have balance 2200.00
    And a transaction record should be created

  Scenario: Employee cannot transfer more than the account's transfer limit
    Given an employee with email "bob@example.com" and password "bob123"
    And a customer with source account "NL12345678901234567890" and balance 10000.00 and transfer limit 1000.00
    And a customer with destination account "NL22223333444455556666" and balance 2000.00
    When the employee logs in
    And the employee attempts to transfer 2000.00 from "NL12345678901234567890" to "NL22223333444455556666"
    Then the transfer should be rejected with message containing "exceed daily limit"
    And the source account "NL12345678901234567890" should have balance 10000.00
    And the destination account "NL22223333444455556666" should have balance 2000.00

  Scenario: Employee cannot transfer from an account with insufficient funds
    Given an employee with email "bob@example.com" and password "bob123"
    And a customer with source account "NL00001111222233334444" and balance 100.00
    And a customer with destination account "NL44556677889900112233" and balance 1000.00
    When the employee logs in
    And the employee attempts to transfer 500.00 from "NL00001111222233334444" to "NL44556677889900112233"
    Then the transfer should be rejected with message containing "Insufficient funds"
    And the source account "NL00001111222233334444" should have balance 100.00
    And the destination account "NL44556677889900112233" should have balance 1000.00

  Scenario: Employee can update account transfer limit
    Given an employee with email "bob@example.com" and password "bob123"
    And a customer with source account "NL09876543210987654321" and balance 5000.00 and transfer limit 10000.00
    When the employee logs in
    And the employee updates the transfer limit for account "NL09876543210987654321" to 15000.00
    Then the account "NL09876543210987654321" should have a transfer limit of 15000.00