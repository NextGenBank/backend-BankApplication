package com.nextgenbank.backend.steps;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgenbank.backend.model.*;
import com.nextgenbank.backend.model.dto.TransactionDto;
import com.nextgenbank.backend.repository.AccountRepository;
import com.nextgenbank.backend.repository.UserRepository;
import com.nextgenbank.backend.security.UserPrincipal;
import io.cucumber.java.After;
import io.cucumber.java.en.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

/**
 * Step definitions for ATM transactions.
 *
 * Scenarios in atm.feature are tagged with @atm-test.
 * After each such scenario, the Spring context is marked dirty
 * via @DirtiesContext, ensuring a fresh context for the next run.
 */
public class AtmStepDefinitions {

    @Autowired
    private WebApplicationContext wac;
    // Web application context used to initialize MockMvc

    @Autowired
    private ObjectMapper objectMapper;
    // Jackson ObjectMapper for serializing DTOs to JSON

    @Autowired
    private UserRepository userRepository;
    // Repository for creating and finding users

    @Autowired
    private AccountRepository accountRepository;
    // Repository for creating accounts and checking balances

    @Autowired
    private PasswordEncoder passwordEncoder;
    // Password encoder to hash user passwords

    private MockMvc mockMvc;
    // MockMvc client for performing HTTP requests in tests

    private ResultActions lastResult;
    // Holds the result of the last HTTP request for assertions

    /**
     * Given step: prepare a clean ATM test environment.
     * Builds a MockMvc instance with Spring Security enabled.
     */
    @Given("the ATM test environment is clean and ready")
    public void the_atm_test_environment_is_clean_and_ready() {
        this.mockMvc = webAppContextSetup(wac)
                .apply(springSecurity())
                .build();
    }

    /**
     * After hook for @atm-test scenarios.
     * Marks the context as dirty so that it will be reloaded.
     * This prevents ATM-specific cleanup from affecting other tests.
     */
    @After("@atm-test")
    @DirtiesContext
    public void afterAtmScenario() {
        // No implementation needed; @DirtiesContext triggers context reset
    }

    /**
     * Given step: register a customer with the given email and password,
     * if they do not already exist in the database.
     */
    @Given("a customer {string} with password {string} is registered")
    public void a_customer_with_password_is_registered(String email, String password) {
        if (userRepository.findByEmail(email).isEmpty()) {
            User customer = new User();
            customer.setEmail(email);
            customer.setFirstName(email.split("@")[0]);
            customer.setLastName("User");
            customer.setPassword(passwordEncoder.encode(password));
            customer.setRole(UserRole.CUSTOMER);
            customer.setStatus(UserStatus.APPROVED);
            customer.setBsnNumber(String.valueOf(System.nanoTime()));
            customer.setPhoneNumber(String.valueOf(System.nanoTime()));
            customer.setCreatedAt(LocalDateTime.now());
            userRepository.save(customer);
        }
    }

    /**
     * Given step: create an account with the specified IBAN and balance
     * for the customer identified by email.
     */
    @Given("the customer {string} has an account {string} with a balance of {double}")
    public void the_customer_has_an_account_with_a_balance_of(String email, String iban, double balance) {
        User customer = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found: " + email));
        Account account = new Account();
        account.setIBAN(iban);
        account.setCustomer(customer);
        account.setBalance(BigDecimal.valueOf(balance).setScale(2, RoundingMode.HALF_UP));
        account.setAccountType(AccountType.CHECKING);
        account.setCreatedAt(LocalDateTime.now());
        accountRepository.save(account);
    }

    /**
     * When step: deposit the given amount into the specified account via ATM.
     * Sends a POST to /api/transactions/atm with a DEPOSIT DTO.
     */
    @When("the customer {string} deposits {double} into account {string} via ATM")
    public void the_customer_deposits_into_account_via_atm(String email, double amount, String iban) throws Exception {
        TransactionDto dto = new TransactionDto();
        dto.setTransactionType(TransactionType.DEPOSIT);
        dto.setToIban(iban);
        dto.setAmount(BigDecimal.valueOf(amount));
        User initiator = userRepository.findByEmail(email).orElseThrow();
        lastResult = mockMvc.perform(post("/api/transactions/atm")
                .with(user(new UserPrincipal(initiator)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)));
    }

    /**
     * When step: withdraw the given amount from the specified account via ATM.
     * Delegates to performWithdraw.
     */
    @When("the customer {string} withdraws {double} from account {string} via ATM")
    public void the_customer_withdraws_from_account_via_atm(String email, double amount, String iban) throws Exception {
        performWithdraw(email, amount, iban);
    }

    /**
     * When step: attempt to withdraw (possibly insufficient) via ATM.
     * Also delegates to performWithdraw.
     */
    @When("the customer {string} attempts to withdraw {double} from account {string} via ATM")
    public void the_customer_attempts_to_withdraw_from_account_via_atm(String email, double amount, String iban) throws Exception {
        performWithdraw(email, amount, iban);
    }

    /**
     * Helper method to perform a withdrawal POST request.
     */
    private void performWithdraw(String email, double amount, String iban) throws Exception {
        TransactionDto dto = new TransactionDto();
        dto.setTransactionType(TransactionType.WITHDRAWAL);
        dto.setFromIban(iban);
        dto.setAmount(BigDecimal.valueOf(amount));
        User initiator = userRepository.findByEmail(email).orElseThrow();
        lastResult = mockMvc.perform(post("/api/transactions/atm")
                .with(user(new UserPrincipal(initiator)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)));
    }

    /**
     * Then step: assert that the operation succeeded with the given HTTP status.
     */
    @Then("the ATM operation is successful with HTTP status {int}")
    public void the_atm_operation_is_successful_with_http_status(int statusCode) throws Exception {
        lastResult.andExpect(status().is(statusCode));
    }

    /**
     * Then step: assert that the operation failed with the given HTTP status.
     */
    @Then("the ATM operation fails with HTTP status {int}")
    public void the_atm_operation_fails_with_http_status(int statusCode) throws Exception {
        lastResult.andExpect(status().is(statusCode));
    }

    /**
     * Then step: check that the JSON response field transactionType matches.
     */
    @Then("the response shows the transaction type as {string}")
    public void the_response_shows_the_transaction_type_as(String type) throws Exception {
        lastResult.andExpect(jsonPath("$.transactionType", is(type)));
    }

    /**
     * Then step: verify the new account balance in the database.
     */
    @Then("the new balance of account {string} is {double}")
    public void the_new_balance_of_account_is(String iban, double newBalance) {
        Account updated = accountRepository.findById(iban)
                .orElseThrow(() -> new AssertionError("Account not found: " + iban));
        BigDecimal expected = BigDecimal.valueOf(newBalance).setScale(2, RoundingMode.HALF_UP);
        assertEquals(0, expected.compareTo(updated.getBalance()), "Balance mismatch");
    }

    /**
     * Then step: assert that the response body contains the specified error message.
     */
    @Then("the response contains the error message {string}")
    public void the_response_contains_the_error_message(String message) throws Exception {
        lastResult.andExpect(content().string(containsString(message)));
    }
}
