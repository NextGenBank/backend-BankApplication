Feature: IBAN Lookup

  Scenario: Lookup by full name
    Given the user is authenticated as "alice@example.com" with password "alice123"
    When the user searches for IBANs using name "Charlie"
    Then the response should include a user named "Charlie"
    And the response should contain at least one IBAN

  Scenario: Lookup by partial IBAN
    Given the user is authenticated as "alice@example.com" with password "alice123"
    When the user searches for IBANs using partial IBAN "NL12"
    Then the response should include users with IBANs containing "NL12"

  Scenario: Lookup returns no results for unmatched name
    Given the user is authenticated as "alice@example.com" with password "alice123"
    When the user searches for IBANs using name "Zzzz"
    Then the response should be empty

  Scenario: Lookup returns no results for unmatched IBAN
    Given the user is authenticated as "alice@example.com" with password "alice123"
    When the user searches for IBANs using partial IBAN "INVALIDIBAN123"
    Then the response should be empty
