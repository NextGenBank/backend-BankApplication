Feature: Registration edge case validation

  Scenario: Missing all fields
    Given I register with:
      | firstName | [empty]           |
      | lastName  | [empty]           |
      | email     | [empty]           |
      | password  | [empty]           |
      | bsn       | [empty]           |
      | phone     | [empty]           |
    Then the response status should be 400

  Scenario: BSN contains letters
    Given I register with:
      | firstName | John              |
      | lastName  | Doe               |
      | email     | letters@example.com |
      | password  | pass123           |
      | bsn       | abc456789         |
      | phone     | 0612345678        |
    Then the response status should be 400

  Scenario: Phone number contains letters
    Given I register with:
      | firstName | Jane              |
      | lastName  | Doe               |
      | email     | phoneletters@example.com |
      | password  | pass123           |
      | bsn       | 123456789         |
      | phone     | 06abc56789        |
    Then the response status should be 400

  Scenario: First name too long
    Given I register with:
      | firstName | AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA |
      | lastName  | LongName         |
      | email     | longname@example.com |
      | password  | pass123           |
      | bsn       | 123456789         |
      | phone     | 0612345678        |
    Then the response status should be 200

  Scenario: Email with uppercase letters
    Given I register with:
      | firstName | Upper             |
      | lastName  | Case              |
      | email     | UpperCase@EXAMPLE.COM |
      | password  | pass123           |
      | bsn       | 123456789         |
      | phone     | 0612345678        |
    Then the response status should be 200
