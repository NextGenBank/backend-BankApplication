package com.nextgenbank.backend.steps;

import io.cucumber.java.Before;
import io.cucumber.java.en.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import com.nextgenbank.backend.model.User;
import com.nextgenbank.backend.model.Account;
import com.nextgenbank.backend.model.Transaction;
import com.nextgenbank.backend.model.AccountType;
import com.nextgenbank.backend.model.UserRole;
import com.nextgenbank.backend.repository.UserRepository;
import com.nextgenbank.backend.repository.AccountRepository;
import com.nextgenbank.backend.repository.TransactionRepository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

public class ATMStepDefinitions {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired private UserRepository userRepo;
    @Autowired private AccountRepository accountRepo;
    @Autowired private TransactionRepository txRepo;

    private ResponseEntity<String> response;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void cleanDb() {
        txRepo.deleteAll();
        accountRepo.deleteAll();
        userRepo.deleteAll();
    }

    @Given("a user {string} with IBAN {string} and balance {double}")
    public void createUserWithAccount(String fullName, String iban, Double balance) {
        String[] parts = fullName.split(" ");
        User u = new User();
        u.setFirstName(parts[0]);
        u.setLastName(parts[1]);
        u.setEmail(parts[0].toLowerCase() + "@example.com");
        u.setPassword("pass");
        u.setRole(UserRole.CUSTOMER);
        u = userRepo.save(u);

        Account a = new Account();
        a.setIBAN(iban);
        a.setAccountType(AccountType.CHECKING);
        a.setBalance(BigDecimal.valueOf(balance));
        a.setCreatedBy(u);
        a.setCustomer(u);
        accountRepo.save(a);
    }

    @When("I POST to {string} with body")
    public void iPostToWithBody(String url, String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        response = restTemplate.postForEntity(
                url,
                new HttpEntity<>(body.trim(), headers),
                String.class
        );
    }

    @Then("the response status code should be {int}")
    public void theResponseStatusCodeShouldBe(int code) {
        assertEquals(code, response.getStatusCodeValue());
    }

    @Then("the response body should contain {string}")
    public void theResponseBodyShouldContain(String fragment) {
        assertTrue(response.getBody().contains(fragment));
    }

    @Then("the JSON field {string} should be {string}")
    public void theJsonFieldShouldBe(String field, String expected) throws Exception {
        JsonNode root = objectMapper.readTree(response.getBody());
        assertEquals(expected, root.get(field).asText());
    }
}
