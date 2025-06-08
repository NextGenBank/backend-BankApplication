Feature: User Registration

  Scenario: Registering with a new email
    Given I register with:
      | firstName | Test                    |
      | lastName  | User                    |
      | email     | newuser123@example.com |
      | password  | mypass123               |
      | bsn       | 999888776               |
      | phone     | 0612348999              |
    Then the response status should be 200
    And the user with email "newuser123@example.com" should exist in the database
