package com.nextgenbank.backend.steps;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgenbank.backend.model.*;
import com.nextgenbank.backend.repository.AccountRepository;
import com.nextgenbank.backend.repository.TransactionRepository;
import com.nextgenbank.backend.repository.UserRepository;
import com.nextgenbank.backend.security.JwtProvider;
import io.cucumber.java.en.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.boot.test.web.client.TestRestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ATMStepDefinition {

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private TransactionRepository transactionRepository;
    @Autowired
    private JwtProvider jwtProvider;

    private HttpHeaders headers = new HttpHeaders();
    private ResponseEntity<String> lastResponse;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private User currentUser;

    @Given("a user {string} with IBAN {string} and balance {double}")
    public void a_user_with_iban_and_balance(String name, String iban, Double balance) {
        String[] p = name.split(" ");
        String first = p[0], last = p.length>1?p[1]:"";
        String email = first.toLowerCase()+ UUID.randomUUID()+"@test.com";
        User u = new User();
        u.setFirstName(first);
        u.setLastName(last);
        u.setEmail(email);
        u.setPassword("pwd");
        u.setPhoneNumber(UUID.randomUUID().toString().substring(0,10));
        u.setBsnNumber(UUID.randomUUID().toString().substring(0,9));
        u.setRole(UserRole.CUSTOMER);
        u.setStatus(UserStatus.APPROVED);
        u.setCreatedAt(LocalDateTime.now());
        userRepository.save(u);
        Account a = new Account();
        a.setIBAN(iban);
        a.setCustomer(u);
        a.setAccountType(AccountType.CHECKING);
        a.setBalance(BigDecimal.valueOf(balance));
        a.setAbsoluteTransferLimit(BigDecimal.valueOf(10000));
        a.setDailyTransferAmount(BigDecimal.ZERO);
        a.setCreatedAt(LocalDateTime.now());
        a.setCreatedBy(u);
        accountRepository.save(a);
        currentUser = u;
    }

    @Given("I am authenticated as {string}")
    public void i_am_authenticated_as(String ignored) {
        String token = jwtProvider.generateToken(currentUser);
        headers.setBearerAuth(token);
    }

    @When("^I POST to \"([^\"]*)\" with body (\\{.*\\})$")
    public void i_post_to_with_body(String url, String body) {
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> req = new HttpEntity<>(body, headers);
        lastResponse = restTemplate.postForEntity(url, req, String.class);
    }

    @Then("the response status code should be {int}")
    public void the_response_status_code_should_be(int code) {
        assertEquals(code, lastResponse.getStatusCodeValue());
    }

    @Then("the response body should contain {string}")
    public void the_response_body_should_contain(String text) {
        assertTrue(lastResponse.getBody()!=null && lastResponse.getBody().contains(text));
    }

    @Then("the account balance for IBAN {string} should be {double}")
    public void the_account_balance_for_iban_should_be(String iban, Double bal) {
        Account a = accountRepository.findById(iban).orElseThrow();
        assertEquals(0, a.getBalance().compareTo(BigDecimal.valueOf(bal)));
    }

    @Then("the JSON field {string} should be {string}")
    public void the_json_field_should_be(String field, String expected) throws Exception {
        JsonNode n = objectMapper.readTree(lastResponse.getBody());
        String actual = n.has(field)&&!n.get(field).isNull() ? n.get(field).asText() : "null";
        assertEquals(expected, actual);
    }

    @Then("there should be a transaction with type {string}, amount {double}, fromIban {string}, toIban {string}")
    public void there_should_be_a_transaction_with_type_amount_fromiban_toiban(
            String type, Double amt, String fromIban, String toIban
    ) {
        boolean ok = transactionRepository.findAll().stream().anyMatch(tx -> {
            boolean t = tx.getTransactionType().name().equalsIgnoreCase(type);
            boolean a = tx.getAmount().compareTo(BigDecimal.valueOf(amt))==0;
            boolean f = (fromIban.equals("null")&&tx.getFromAccount()==null)
                    || (tx.getFromAccount()!=null&&fromIban.equals(tx.getFromAccount().getIBAN()));
            boolean to = (toIban.equals("null")&&tx.getToAccount()==null)
                    || (tx.getToAccount()!=null&&toIban.equals(tx.getToAccount().getIBAN()));
            return t&&a&&f&&to;
        });
        assertTrue(ok);
    }
}
