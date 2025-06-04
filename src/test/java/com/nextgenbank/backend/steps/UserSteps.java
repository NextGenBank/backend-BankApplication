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

        // assert successful login
        assertEquals(200, response.getStatusCodeValue());

        // Extract JWT
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
        //  no assert, we expect to check status later
    }

    @When("I GET {string}")
    public void i_get(String endpoint) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(jwtToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        response = restTemplate.exchange(endpoint, HttpMethod.GET, request, String.class);
    }

    // Registration Tests

    @Given("I register with:")
    public void i_register_with(io.cucumber.datatable.DataTable dataTable) {
        Map<String, String> data = dataTable.asMap();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String body = String.format("""
            {
              "firstName": "%s",
              "lastName": "%s",
              "email": "%s",
              "password": "%s",
              "bsn": "%s",
              "phone": "%s"
            }
        """,
                data.get("firstName"), data.get("lastName"), data.get("email"),
                data.get("password"), data.get("bsn"), data.get("phone"));

        HttpEntity<String> request = new HttpEntity<>(body, headers);
        response = restTemplate.postForEntity("/auth/register", request, String.class);
    }

    // Response check

    @Then("the response status should be {int}")
    public void the_response_status_should_be(Integer status) {
        assertEquals(status, response.getStatusCodeValue());
    }
}
