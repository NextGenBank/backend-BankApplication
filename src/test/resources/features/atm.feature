# language: en
@atm-test
Feature: ATM Transactions

  As a bank customer
  I want to use an ATM to deposit and withdraw money
  So that I can manage my funds easily.

  Background:
    Given the ATM test environment is clean and ready

  Scenario: Successful deposit into an account
    Given a customer "john.doe@example.com" with password "password123" is registered
    And the customer "john.doe@example.com" has an account "NL01NEXT0001" with a balance of 1000.00
    When the customer "john.doe@example.com" deposits 250.50 into account "NL01NEXT0001" via ATM
    Then the ATM operation is successful with HTTP status 200
    And the response shows the transaction type as "DEPOSIT"
    And the new balance of account "NL01NEXT0001" is 1250.50

  Scenario: Successful withdrawal from an account
    Given a customer "john.doe@example.com" with password "password123" is registered
    And the customer "john.doe@example.com" has an account "NL01NEXT0001" with a balance of 1000.00
    When the customer "john.doe@example.com" withdraws 300.00 from account "NL01NEXT0001" via ATM
    Then the ATM operation is successful with HTTP status 200
    And the response shows the transaction type as "WITHDRAWAL"
    And the new balance of account "NL01NEXT0001" is 700.00

  Scenario: Withdrawal with insufficient funds
    Given a customer "john.doe@example.com" with password "password123" is registered
    And the customer "john.doe@example.com" has an account "NL01NEXT0001" with a balance of 100.00
    When the customer "john.doe@example.com" attempts to withdraw 200.00 from account "NL01NEXT0001" via ATM
    Then the ATM operation fails with HTTP status 400
    And the response contains the error message "Insufficient funds."
    And the new balance of account "NL01NEXT0001" is 100.00
