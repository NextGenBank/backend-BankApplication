package com.nextgenbank.backend.steps;

import io.cucumber.java.en.*;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

public class UserSteps {

    private final TestRestTemplate restTemplate;
    private ResponseEntity<String> response;
    private String jwtToken;

    public UserSteps(TestRestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // Login Tests

    @Given("I successfully log in with email {string} and password {string}")
    public void i_log_in_successfully(String email, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String body = String.format("""
        {
          "email": "%s",
          "password": "%s"
        }
        """, email, password);

        HttpEntity<String> request = new HttpEntity<>(body, headers);
        response = restTemplate.postForEntity("/auth/login", request, String.class);

        assertEquals(200, response.getStatusCodeValue());

        String responseBody = response.getBody();
        int start = responseBody.indexOf(":\"") + 2;
        int end = responseBody.indexOf("\",");
        jwtToken = responseBody.substring(start, end);
    }

    @Given("I attempt to log in with email {string} and password {string}")
    public void i_attempt_login(String email, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String body = String.format("""
        {
          "email": "%s",
          "password": "%s"
        }
        """, email, password);

        HttpEntity<String> request = new HttpEntity<>(body, headers);
        response = restTemplate.postForEntity("/auth/login", request, String.class);
    }
//
//    @When("I GET {string}")
//    public void i_get(String endpoint) {
//        HttpHeaders headers = new HttpHeaders();
//        headers.setBearerAuth(jwtToken);
//        HttpEntity<Void> request = new HttpEntity<>(headers);
//
//        response = restTemplate.exchange(endpoint, HttpMethod.GET, request, String.class);
//    }
@When("I GET {string}")
public void i_get(String endpoint) {
    HttpHeaders headers = new HttpHeaders();

    if (jwtToken != null && !jwtToken.isBlank()) {
        headers.setBearerAuth(jwtToken); // Only send Authorization if logged in
    }

    HttpEntity<Void> request = new HttpEntity<>(headers);
    response = restTemplate.exchange(endpoint, HttpMethod.GET, request, String.class);
}

    // Registration Tests

    @Given("I register with:")
    public void i_register_with(io.cucumber.datatable.DataTable dataTable) {
        Map<String, String> data = dataTable.asMap();

        // Convert [empty] to ""
        String firstName = convertEmpty(data.get("firstName"));
        String lastName = convertEmpty(data.get("lastName"));
        String email = convertEmpty(data.get("email"));
        String password = convertEmpty(data.get("password"));
        String bsn = convertEmpty(data.get("bsn"));
        String phone = convertEmpty(data.get("phone"));

        String json = String.format("""
        {
          "firstName": "%s",
          "lastName": "%s",
          "email": "%s",
          "password": "%s",
          "bsn": "%s",
          "phone": "%s"
        }
        """, firstName, lastName, email, password, bsn, phone);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> request = new HttpEntity<>(json, headers);
        response = restTemplate.postForEntity("/auth/register", request, String.class);
    }

    private String convertEmpty(String value) {
        if (value == null || value.equalsIgnoreCase("[empty]")) {
            return "";
        }
        return value.trim();
    }


    // Response Check

    @Then("the response status should be {int}")
    public void the_response_status_should_be(Integer status) {
        assertEquals(status, response.getStatusCodeValue());
    }

    @Then("a JWT token should be returned")
    public void a_jwt_token_should_be_returned() {
        assertNotNull(jwtToken, "JWT token should not be null");
        assertTrue(jwtToken.startsWith("ey"), "Token should start like a real JWT");
    }

    @Then("the user with email {string} should exist in the database")
    public void the_user_should_exist(String email) {
        ResponseEntity<Boolean> result = restTemplate.getForEntity(
                "/api/test/user-exists?email=" + email, Boolean.class
        );

        assertEquals(200, result.getStatusCodeValue());
        assertTrue(Boolean.TRUE.equals(result.getBody()), "User with email " + email + " should exist");
    }
    @Then("the response should contain {string}")
    public void the_response_should_contain(String expectedContent) {
        assertNotNull(response.getBody());
        assertTrue(response.getBody().toLowerCase().contains(expectedContent.toLowerCase()),
                "Response should contain (case-insensitive): " + expectedContent + "\n\nActual response:\n" + response.getBody());
    }

    @Then("the response should contain a transaction with type {string}")
    public void the_response_should_contain_transaction_type(String type) {
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains("\"transactionType\":\"" + type + "\""),
                "Expected transaction type: " + type + "\nActual response:\n" + response.getBody());
    }

    @Then("the response should include both {string} and {string}")
    public void the_response_should_include_both(String field1, String field2) {
        assertNotNull(response.getBody());
        assertTrue(response.getBody().contains(field1),
                "Expected field: " + field1 + " not found.\nResponse:\n" + response.getBody());
        assertTrue(response.getBody().contains(field2),
                "Expected field: " + field2 + " not found.\nResponse:\n" + response.getBody());
    }


}
