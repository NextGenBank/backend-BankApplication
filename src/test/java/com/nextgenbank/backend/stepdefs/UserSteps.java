package com.nextgenbank.backend.stepdefs;

import io.cucumber.java.en.*;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import static org.junit.jupiter.api.Assertions.*;

public class UserSteps {

    private final TestRestTemplate restTemplate = new TestRestTemplate();
    private ResponseEntity<String> response;
    private String jwtToken;

    @Given("I log in with email {string} and password {string}")
    public void i_log_in_with_email_and_password(String email, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String body = String.format("""
            {
              "email": "%s",
              "password": "%s"
            }
        """, email, password);

        HttpEntity<String> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> loginResponse = restTemplate.postForEntity("http://localhost:8080/auth/login", request, String.class);
        assertEquals(200, loginResponse.getStatusCodeValue());

        // Extract JWT token from JSON
        String responseBody = loginResponse.getBody();
        int start = responseBody.indexOf(":\"") + 2;
        int end = responseBody.indexOf("\",");
        jwtToken = responseBody.substring(start, end);
    }

    @When("I GET {string}")
    public void i_get(String endpoint) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        response = restTemplate.exchange("http://localhost:8080" + endpoint, HttpMethod.GET, request, String.class);
    }

    @Then("the response status should be {int}")
    public void the_response_status_should_be(Integer status) {
        assertEquals(status, response.getStatusCodeValue());
    }
}
