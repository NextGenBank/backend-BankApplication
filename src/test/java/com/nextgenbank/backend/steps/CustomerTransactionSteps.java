package com.nextgenbank.backend.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgenbank.backend.model.dto.TransactionResponseDto;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CustomerTransactionSteps {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private ResponseEntity<String> latestResponse;
    private String authToken;

    private List<TransactionResponseDto> extractTransactionContent() throws Exception {
        JsonNode json = objectMapper.readTree(latestResponse.getBody());
        JsonNode contentNode = json.get("content");
        return objectMapper.readerForListOf(TransactionResponseDto.class).readValue(contentNode);
    }

    @Given("a registered customer with email {string} and password {string}")
    public void a_registered_customer_with_email_and_password(String email, String password) {
        // Assume user already exists
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
        assertNotNull(latestResponse);
        assertEquals(HttpStatus.OK, latestResponse.getStatusCode());

        try {
            List<TransactionResponseDto> transactions = extractTransactionContent();
            assertTrue(transactions.size() >= count, "Expected at least " + count + " transactions");
        } catch (Exception e) {
            fail("Failed to parse paginated transactions: " + e.getMessage());
        }
    }

    @Then("the response should contain {int} transactions")
    public void the_response_should_contain_transactions(Integer expectedCount) {
        assertNotNull(latestResponse);
        assertEquals(HttpStatus.OK, latestResponse.getStatusCode());

        try {
            List<TransactionResponseDto> transactions = extractTransactionContent();
            assertEquals(expectedCount, transactions.size());
        } catch (Exception e) {
            fail("Failed to parse paginated transactions: " + e.getMessage());
        }
    }

    @When("the customer tries to request transactions without a token")
    public void the_customer_tries_to_request_transactions_without_a_token() {
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        latestResponse = restTemplate.exchange("/api/transactions", HttpMethod.GET, entity, String.class);
    }

    @Then("the response should be unauthorized")
    public void the_response_should_be_unauthorized() {
        assertNotNull(latestResponse);
        assertEquals(HttpStatus.UNAUTHORIZED, latestResponse.getStatusCode());
    }

    @Then("the response should contain only transactions belonging to {string}")
    public void the_response_should_contain_only_transactions_belonging_to(String expectedName) {
        assertNotNull(latestResponse);
        assertEquals(HttpStatus.OK, latestResponse.getStatusCode());

        try {
            List<TransactionResponseDto> transactions = extractTransactionContent();

            for (TransactionResponseDto txn : transactions) {
                switch (txn.direction().toUpperCase()) {
                    case "OUTGOING" -> assertEquals(expectedName, txn.fromName());
                    case "INCOMING" -> assertEquals(expectedName, txn.toName());
                    case "INTERNAL" -> assertTrue(expectedName.equals(txn.fromName()) || expectedName.equals(txn.toName()));
                    default -> fail("Unexpected transaction direction: " + txn.direction());
                }
            }
        } catch (Exception e) {
            fail("Failed to parse transactions: " + e.getMessage());
        }
    }

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
            List<TransactionResponseDto> transactions = extractTransactionContent();

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
            List<TransactionResponseDto> transactions = extractTransactionContent();

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
            List<TransactionResponseDto> transactions = extractTransactionContent();

            for (TransactionResponseDto txn : transactions) {
                assertEquals(type.toUpperCase(), txn.direction().toUpperCase());
            }
        } catch (Exception e) {
            fail("Failed to parse transactions: " + e.getMessage());
        }
    }
}
