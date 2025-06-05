Feature: User Registration

  Scenario: Registering with a new email
    Given I register with:
      | firstName | Test                    |
      | lastName  | User                    |
      | email     | testuser453@example.com |
      | password  | mypass123               |
      | bsn       | 123456789               |
      | phone     | 0612345678              |
    Then the response status should be 200
    And the user with email "testuser453@example.com" should exist in the database
