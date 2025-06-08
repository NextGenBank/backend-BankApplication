Feature: Registering with duplicate email

  Scenario: Duplicate email registration
    Given I register with:
      | firstName | John                    |
      | lastName  | Doe                     |
      | email     | duplicate@example.com   |
      | password  | pass123                 |
      | bsn       | 111222334               |
      | phone     | 0612345678              |
    Then the response status should be 200

    Given I register with:
      | firstName | Duplicate               |
      | lastName  | User                    |
      | email     | duplicate@example.com   |
      | password  | pass456                 |
      | bsn       | 444555667               |
      | phone     | 0612345679              |
    Then the response status should be 400
