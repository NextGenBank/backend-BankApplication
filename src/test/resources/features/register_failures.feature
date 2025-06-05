Feature: Registration validation errors

  Scenario: Missing first name
    Given I register with:
      | firstName |                         |
      | lastName  | User                    |
      | email     | user1@example.com       |
      | password  | pass123                 |
      | bsn       | 123456789               |
      | phone     | 0612345678              |
    Then the response status should be 400

  Scenario: Invalid email format
    Given I register with:
      | firstName | Invalid                 |
      | lastName  | Email                   |
      | email     | not-an-email            |
      | password  | pass123                 |
      | bsn       | 123456789               |
      | phone     | 0612345678              |
    Then the response status should be 400

  Scenario: Short password
    Given I register with:
      | firstName | Short                   |
      | lastName  | Pass                    |
      | email     | short@example.com       |
      | password  | 123                     |
      | bsn       | 123456789               |
      | phone     | 0612345678              |
    Then the response status should be 400

  Scenario: Invalid BSN
    Given I register with:
      | firstName | InvalidBSN              |
      | lastName  | Test                    |
      | email     | bsn@example.com         |
      | password  | pass123                 |
      | bsn       | 123                     |
      | phone     | 0612345678              |
    Then the response status should be 400

  Scenario: Invalid phone number
    Given I register with:
      | firstName | InvalidPhone            |
      | lastName  | Test                    |
      | email     | phone@example.com       |
      | password  | pass123                 |
      | bsn       | 123456789               |
      | phone     | 0712345678              |
    Then the response status should be 400
