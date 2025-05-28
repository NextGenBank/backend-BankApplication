Feature: View current user

  Scenario: Authenticated user accesses profile
    Given I log in with email "alice@example.com" and password "alice123"
    When I GET "/api/user/me"
    Then the response status should be 200
