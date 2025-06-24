Feature: Employee customer management

  Scenario: Employee can view all customers with pagination
    Given an employee with email "bob@example.com" and password "bob123"
    When the employee logs in
    And the employee requests the first page of customers
    Then the response should contain a paginated list of customers
    And the pagination metadata should be correct

  Scenario: Employee can view customers by status
    Given an employee with email "bob@example.com" and password "bob123"
    When the employee logs in
    And the employee requests customers with status "PENDING"
    Then the response should only contain customers with "PENDING" status
    And the pagination metadata should be correct

  Scenario: Employee can approve a pending customer
    Given an employee with email "bob@example.com" and password "bob123"
    # Penderson from DataInitializer is a PENDING user
    When the employee logs in
    And the employee approves the customer with id 2
    Then the customer with id 2 should have status "APPROVED"
    And accounts should be created for the customer

  Scenario: Employee can reject a pending customer
    Given an employee with email "bob@example.com" and password "bob123"
    # Kevin from DataInitializer is a PENDING user
    When the employee logs in
    And the employee rejects the customer with id 3
    Then the customer with id 3 should have status "REJECTED"