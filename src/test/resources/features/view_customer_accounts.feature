Feature: View customer accounts

  Scenario: Authenticated customer views their accounts
    Given I successfully log in with email "alice@example.com" and password "alice123"
    When I GET "/api/accounts/my"
    Then the response status should be 200
    And the response should contain "iban"
    And the response should contain "accountType"
    And the response should contain "balance"

