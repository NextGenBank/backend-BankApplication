package com.nextgenbank.backend.steps;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgenbank.backend.model.dto.TransactionResponseDto;
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
        // Assume user already created in DataInitializer or test setup
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
            fail("Failed to extract token from login response: " + e.getMessage());
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
            List<TransactionResponseDto> transactions = objectMapper.readValue(
                    latestResponse.getBody(),
                    new TypeReference<>() {}
            );
            assertNotNull(transactions);
            assertTrue(transactions.size() >= count, "Expected at least " + count + " transactions");
        } catch (Exception e) {
            fail("Failed to parse transactions from response: " + e.getMessage());
        }
    }

    @Then("the response should contain {int} transactions")
    public void the_response_should_contain_transactions(Integer expectedCount) {
        assertNotNull(latestResponse, "No response received");
        assertEquals(HttpStatus.OK, latestResponse.getStatusCode());

        try {
            List<TransactionResponseDto> transactions = objectMapper.readValue(
                    latestResponse.getBody(),
                    new TypeReference<>() {}
            );
            assertEquals(expectedCount, transactions.size(), "Unexpected number of transactions");
        } catch (Exception e) {
            fail("Failed to parse transactions: " + e.getMessage());
        }
    }

    @When("the customer tries to request transactions without a token")
    public void the_customer_tries_to_request_transactions_without_a_token() {
        HttpHeaders headers = new HttpHeaders(); // No Authorization header
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        latestResponse = restTemplate.exchange("/api/transactions", HttpMethod.GET, entity, String.class);
    }

    @Then("the response should be unauthorized")
    public void the_response_should_be_unauthorized() {
        assertNotNull(latestResponse, "No response received");
        assertEquals(HttpStatus.UNAUTHORIZED, latestResponse.getStatusCode(), "Expected 401 UNAUTHORIZED");
    }

    @Then("the response should contain only transactions belonging to {string}")
    public void the_response_should_contain_only_transactions_belonging_to(String expectedName) {
        assertNotNull(latestResponse, "No response received");
        assertEquals(HttpStatus.OK, latestResponse.getStatusCode(), "Expected 200 OK");

        try {
            List<TransactionResponseDto> transactions = objectMapper.readValue(
                    latestResponse.getBody(),
                    new TypeReference<>() {}
            );

            for (TransactionResponseDto txn : transactions) {
                switch (txn.direction().toUpperCase()) {
                    case "OUTGOING" -> assertEquals(expectedName, txn.fromName(), "Sender name mismatch");
                    case "INCOMING" -> assertEquals(expectedName, txn.toName(), "Receiver name mismatch");
                    case "INTERNAL" -> assertTrue(
                            expectedName.equals(txn.fromName()) || expectedName.equals(txn.toName()),
                            "Expected name not found in internal transaction"
                    );
                    default -> fail("Unexpected transaction direction: " + txn.direction());
                }
            }

        } catch (Exception e) {
            fail("Failed to parse transactions: " + e.getMessage());
        }
    }

    // filter transactions

    @When("the customer filters transactions by IBAN {string}")
    public void the_customer_filters_transactions_by_iban(String iban) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        latestResponse = restTemplate.exchange(
                "/api/transactions?iban=" + iban, HttpMethod.GET, entity, String.class);
    }

    @Then("the response should contain only transactions involving IBAN {string}")
    public void the_response_should_contain_only_transactions_involving_iban(String iban) {
        assertNotNull(latestResponse);
        assertEquals(HttpStatus.OK, latestResponse.getStatusCode());

        try {
            List<TransactionResponseDto> transactions = objectMapper.readValue(
                    latestResponse.getBody(),
                    new TypeReference<>() {}
            );

            for (TransactionResponseDto txn : transactions) {
                assertTrue(txn.fromIban().equals(iban) || txn.toIban().equals(iban),
                        "Transaction does not involve expected IBAN: " + iban);
            }
        } catch (Exception e) {
            fail("Failed to parse transactions: " + e.getMessage());
        }
    }

    @When("the customer filters transactions by name {string}")
    public void the_customer_filters_transactions_by_name(String name) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        latestResponse = restTemplate.exchange(
                "/api/transactions?name=" + name, HttpMethod.GET, entity, String.class);
    }

    @Then("the response should contain only transactions involving name {string}")
    public void the_response_should_contain_only_transactions_involving_name(String name) {
        assertNotNull(latestResponse);
        assertEquals(HttpStatus.OK, latestResponse.getStatusCode());

        try {
            List<TransactionResponseDto> transactions = objectMapper.readValue(
                    latestResponse.getBody(),
                    new TypeReference<>() {}
            );

            for (TransactionResponseDto txn : transactions) {
                boolean fromMatch = txn.fromName().toLowerCase().contains(name.toLowerCase());
                boolean toMatch = txn.toName().toLowerCase().contains(name.toLowerCase());
                assertTrue(fromMatch || toMatch, "Transaction does not involve expected name: " + name);
            }
        } catch (Exception e) {
            fail("Failed to parse transactions: " + e.getMessage());
        }
    }

    @When("the customer filters transactions by type {string}")
    public void the_customer_filters_transactions_by_type(String type) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        latestResponse = restTemplate.exchange(
                "/api/transactions?type=" + type, HttpMethod.GET, entity, String.class);
    }

    @Then("the response should contain only transactions of type {string}")
    public void the_response_should_contain_only_transactions_of_type(String type) {
        assertNotNull(latestResponse);
        assertEquals(HttpStatus.OK, latestResponse.getStatusCode());

        try {
            List<TransactionResponseDto> transactions = objectMapper.readValue(
                    latestResponse.getBody(),
                    new TypeReference<>() {}
            );

            for (TransactionResponseDto txn : transactions) {
                assertEquals(type.toUpperCase(), txn.direction().toUpperCase(),
                        "Transaction type mismatch. Expected: " + type + ", Found: " + txn.direction());
            }
        } catch (Exception e) {
            fail("Failed to parse transactions: " + e.getMessage());
        }
    }

}
