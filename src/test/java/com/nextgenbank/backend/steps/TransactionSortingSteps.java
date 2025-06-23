package com.nextgenbank.backend.steps;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgenbank.backend.model.dto.TransactionResponseDto;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    @Given("the customer logs in for sorting tests with email {string} and password {string}")
    public void the_customer_logs_in_for_sorting_tests(String email, String password) {
        String loginRequest = String.format("{\"email\":\"%s\", \"password\":\"%s\"}", email, password);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(loginRequest, headers);
        latestResponse = restTemplate.postForEntity("/auth/login", entity, String.class);

        try {
            //ObjectMapper to parse the JSON response and extract the token field, I sote the token in the authToken for future requests
            authToken = objectMapper.readTree(latestResponse.getBody()).get("token").asText();
        } catch (Exception e) {
            fail("Failed to extract token: " + e.getMessage());
        }
    }

    private List<TransactionResponseDto> extractTransactionContent() throws Exception {
        JsonNode json = objectMapper.readTree(latestResponse.getBody());
        JsonNode contentNode = json.path("content");
        if (contentNode.isMissingNode()) {
            contentNode = json; // fallback to root if no content node
        }
        return objectMapper.readValue(contentNode.toString(), new TypeReference<>() {});
    }

    @When("the customer requests transactions sorted by type")
    public void the_customer_requests_transactions_sorted_by_type() {
        fetchTransactionsWithSort("transactionType,asc");
    }

    @Then("the transactions should be sorted alphabetically by type")
    public void the_transactions_should_be_sorted_alphabetically_by_type() throws Exception {
        List<TransactionResponseDto> transactions = extractTransactionContent();
        System.out.println("=== Transactions sorted by type ===");
        transactions.forEach(t -> System.out.printf("Type: %s | Amount: %s | Timestamp: %s%n",
                t.transactionType(), t.amount(), t.timestamp()));

        assertFalse(transactions.isEmpty(), "No transactions returned");
        assertTrue(isSortedAscending(transactions, Comparator.comparing(dto -> dto.transactionType().name())), //returns the enum name as a string for comparison
                "Transactions are not sorted by type in ascending order");
    }

    private void fetchTransactionsWithSort(String sortParam) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authToken);
        HttpEntity<Void> request = new HttpEntity<>(headers);

        String url = String.format("/api/transactions?sort=%s", sortParam); //builds the final URL with the query parameter
        latestResponse = restTemplate.exchange(url, HttpMethod.GET, request, String.class);
    }

    private <T> boolean isSortedDescending(List<T> list, Comparator<T> comparator) {
        if (list.isEmpty() || list.size() == 1) {
            return true;
        }

        for (int i = 0; i < list.size() - 1; i++) {
            if (comparator.compare(list.get(i), list.get(i + 1)) < 0) {
                System.err.printf("Sort violation at position %d: %s comes before %s%n",
                        i, list.get(i), list.get(i + 1));
                return false;
            }
        }
        return true;
    }

    private <T> boolean isSortedAscending(List<T> list, Comparator<T> comparator) {
        if (list.isEmpty() || list.size() == 1) {
            return true;
        }

        for (int i = 0; i < list.size() - 1; i++) {
            if (comparator.compare(list.get(i), list.get(i + 1)) > 0) {
                System.err.printf("Sort violation at position %d: %s comes before %s%n",
                        i, list.get(i), list.get(i + 1));
                return false;
            }
        }
        return true;
    }
}