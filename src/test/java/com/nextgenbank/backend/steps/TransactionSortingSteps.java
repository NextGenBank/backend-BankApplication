package com.nextgenbank.backend.steps;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgenbank.backend.model.dto.TransactionResponseDto;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TransactionSortingSteps {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private ResponseEntity<String> latestResponse;
    private String authToken;

    @When("the customer requests transactions sorted by most recent")
    public void the_customer_requests_transactions_sorted_by_most_recent() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        latestResponse = restTemplate.exchange("/api/transactions?sort=recent", HttpMethod.GET, entity, String.class);
    }

    @Then("the transactions should be sorted from newest to oldest")
    public void the_transactions_should_be_sorted_from_newest_to_oldest() throws Exception {
        List<TransactionResponseDto> transactions = objectMapper.readValue(latestResponse.getBody(), new TypeReference<>() {});

        for (int i = 1; i < transactions.size(); i++) {
            assertTrue(transactions.get(i - 1).timestamp().isAfter(transactions.get(i).timestamp()) ||
                    transactions.get(i - 1).timestamp().isEqual(transactions.get(i).timestamp()));
        }
    }

    @When("the customer requests transactions sorted by amount")
    public void the_customer_requests_transactions_sorted_by_amount() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        latestResponse = restTemplate.exchange("/api/transactions?sort=amount", HttpMethod.GET, entity, String.class);
    }

    @Then("the transactions should be sorted from highest to lowest amount")
    public void the_transactions_should_be_sorted_from_highest_to_lowest_amount() throws Exception {
        List<TransactionResponseDto> transactions = objectMapper.readValue(latestResponse.getBody(), new TypeReference<>() {});

        for (int i = 1; i < transactions.size(); i++) {
            BigDecimal prev = transactions.get(i - 1).amount();
            BigDecimal curr = transactions.get(i).amount();
            assertTrue(prev.compareTo(curr) >= 0);
        }
    }

    @When("the customer requests transactions sorted by type")
    public void the_customer_requests_transactions_sorted_by_type() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        latestResponse = restTemplate.exchange("/api/transactions?sort=type", HttpMethod.GET, entity, String.class);
    }

    @Then("the transactions should be sorted alphabetically by type")
    public void the_transactions_should_be_sorted_alphabetically_by_type() throws Exception {
        List<TransactionResponseDto> transactions = objectMapper.readValue(latestResponse.getBody(), new TypeReference<>() {});

        for (int i = 1; i < transactions.size(); i++) {
            String prev = transactions.get(i - 1).transactionType().name();
            String curr = transactions.get(i).transactionType().name();
            assertTrue(prev.compareTo(curr) <= 0);
        }
    }

    @When("the customer logs in for sorting tests with email {string} and password {string}")
    public void the_customer_logs_in_for_sorting_tests(String email, String password) {
        String loginRequest = String.format("{\"email\":\"%s\", \"password\":\"%s\"}", email, password);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(loginRequest, headers);
        latestResponse = restTemplate.postForEntity("/auth/login", entity, String.class);

        try {
            authToken = objectMapper.readTree(latestResponse.getBody()).get("token").asText();
        } catch (Exception e) {
            fail("Failed to extract token: " + e.getMessage());
        }
    }
}
