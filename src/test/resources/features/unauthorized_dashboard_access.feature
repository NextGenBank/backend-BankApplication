Feature: Unauthorized access prevention

  Scenario: Guest user tries to access profile
    When I GET "/api/user/me"
    Then the response status should be 401

  Scenario: Guest user tries to access accounts
    When I GET "/api/accounts/my"
    Then the response status should be 401
