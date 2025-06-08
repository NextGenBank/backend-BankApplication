Feature: Password validation during registration

  Scenario: Password missing number
    Given I register with:
      | firstName | NoNumber              |
      | lastName  | Pass                  |
      | email     | nonumber@example.com  |
      | password  | abcdef                |
      | bsn       | 123456789             |
      | phone     | 0612345678            |
    Then the response status should be 400

  Scenario: Password missing letter
    Given I register with:
      | firstName | NoLetter              |
      | lastName  | Pass                  |
      | email     | noletter@example.com  |
      | password  | 123456                |
      | bsn       | 123456789             |
      | phone     | 0612345678            |
    Then the response status should be 400

  Scenario: Password contains only symbols
    Given I register with:
      | firstName | OnlySymbols           |
      | lastName  | Test                  |
      | email     | symbols@example.com   |
      | password  | @$!%*#                |
      | bsn       | 123456789             |
      | phone     | 0612345678            |
    Then the response status should be 400

  Scenario: Password valid with letters, numbers, and symbols
    Given I register with:
      | firstName | Valid                 |
      | lastName  | Combo                 |
      | email     | validpass@example.com|
      | password  | Ab12a1!               |
      | bsn       | 553456789             |
      | phone     | 0698765433            |
    Then the response status should be 200
    And the user with email "validpass@example.com" should exist in the database
