package com.nextgenbank.backend.steps;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgenbank.backend.model.Transaction;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CustomerTransactionSteps {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private ResponseEntity<String> latestResponse;
    private String authToken;

    @Given("a registered customer with email {string} and password {string}")
    public void a_registered_customer_with_email_and_password(String email, String password) {
        // Assume DataInitializer already registered this user.
        // If dynamic user creation is needed, add registration logic here.
    }

    @Given("the customer logs in with email {string} and password {string}")
    public void the_customer_logs_in_with_email_and_password(String email, String password) {
        String loginUrl = "/auth/login";
        String loginRequest = String.format("{\"email\":\"%s\", \"password\":\"%s\"}", email, password);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> requestEntity = new HttpEntity<>(loginRequest, headers);

        latestResponse = restTemplate.postForEntity(loginUrl, requestEntity, String.class);
        assertEquals(HttpStatus.OK, latestResponse.getStatusCode());

        try {
            String responseBody = latestResponse.getBody();
            authToken = objectMapper.readTree(responseBody).get("token").asText();
        } catch (Exception e) {
            fail("Failed to extract token from login response");
        }
    }

    @When("the customer requests their transaction history")
    public void the_customer_requests_their_transaction_history() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        latestResponse = restTemplate.exchange("/api/transactions", HttpMethod.GET, entity, String.class);
    }

    @Then("the response should contain at least {int} transaction")
    public void the_response_should_contain_at_least_transaction(Integer count) {
        assertNotNull(latestResponse, "No response received");
        assertEquals(HttpStatus.OK, latestResponse.getStatusCode(), "Expected 200 OK");

        try {
            List<Transaction> transactions = objectMapper.readValue(
                    latestResponse.getBody(),
                    new TypeReference<>() {}
            );
            assertNotNull(transactions);
            assertTrue(transactions.size() >= count, "Expected at least " + count + " transactions");
        } catch (Exception e) {
            fail("Failed to parse transactions from response: " + e.getMessage());
        }
    }

}
