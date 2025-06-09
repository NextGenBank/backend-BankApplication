Feature: ATM operations

  Scenario: Successful withdrawal
    Given a user "Ivan Petrov" with IBAN "NL01BANK0001" and balance 1000.00
    When I POST to "/api/atm/withdraw" with body
      """
      {"iban":"NL01BANK0001","amount":200.00}
      """
    Then the response status code should be 200
    And the JSON field "balance" should be "800.00"

  Scenario: Deposit into account
    Given a user "Anna Ivanova" with IBAN "NL01BANK0002" and balance 500.00
    When I POST to "/api/atm/deposit" with body
      """
      {"iban":"NL01BANK0002","amount":150.00}
      """
    Then the response status code should be 200
    And the JSON field "balance" should be "650.00"
