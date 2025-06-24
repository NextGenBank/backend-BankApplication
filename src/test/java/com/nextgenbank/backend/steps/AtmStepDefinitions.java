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
 * This class is self-contained and uses a manual setup for MockMvc to avoid
 * conflicting with other Cucumber test configurations in the project.
 */
public class AtmStepDefinitions {

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;
    private ResultActions lastResult;

    /**
     * Given step: Prepares the test environment before the background steps run.
     * Manually builds a MockMvc instance with Spring Security integration.
     * This is the key to avoiding dependency conflicts.
     */
    @Given("the ATM test environment is clean and ready")
    public void the_atm_test_environment_is_clean_and_ready() {
        this.mockMvc = webAppContextSetup(wac)
                .apply(springSecurity())
                .build();
    }

    /**
     * After hook for scenarios tagged with @atm-test.
     * @DirtiesContext ensures the Spring context is reset after these specific
     * scenarios run, providing complete isolation from other tests.
     */
    @After("@atm-test")
    @DirtiesContext
    public void afterAtmScenario() {
        // The annotation handles the context reset automatically.
    }

    /**
     * Given step: Registers a customer if they don't already exist.
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
     * Given step: Creates an account for a customer with a specific balance.
     * If an account with the same IBAN exists, it will be updated (useful for overriding Background steps).
     */
    @Given("the customer {string} has an account {string} with a balance of {double}")
    public void the_customer_has_an_account_with_a_balance_of(String email, String iban, double balance) {
        User customer = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found: " + email));

        Account account = accountRepository.findById(iban).orElse(new Account());
        account.setIBAN(iban);
        account.setCustomer(customer);
        account.setBalance(BigDecimal.valueOf(balance).setScale(2, RoundingMode.HALF_UP));
        account.setAccountType(AccountType.CHECKING);
        account.setCreatedAt(LocalDateTime.now());
        accountRepository.save(account);
    }

    /**
     * When step: Simulates a customer depositing money via ATM.
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
     * When step: Simulates a customer withdrawing money via ATM.
     */
    @When("the customer {string} withdraws {double} from account {string} via ATM")
    public void the_customer_withdraws_from_account_via_atm(String email, double amount, String iban) throws Exception {
        performWithdraw(email, amount, iban);
    }

    /**
     * When step: Simulates a customer attempting to withdraw money, possibly failing.
     */
    @When("the customer {string} attempts to withdraw {double} from account {string} via ATM")
    public void the_customer_attempts_to_withdraw_from_account_via_atm(String email, double amount, String iban) throws Exception {
        performWithdraw(email, amount, iban);
    }

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

    @Then("the ATM operation is successful with HTTP status {int}")
    public void the_atm_operation_is_successful_with_http_status(int statusCode) throws Exception {
        lastResult.andExpect(status().is(statusCode));
    }

    @Then("the ATM operation fails with HTTP status {int}")
    public void the_atm_operation_fails_with_http_status(int statusCode) throws Exception {
        lastResult.andExpect(status().is(statusCode));
    }

    @Then("the response shows the transaction type as {string}")
    public void the_response_shows_the_transaction_type_as(String type) throws Exception {
        lastResult.andExpect(jsonPath("$.transactionType", is(type)));
    }

    @Then("the new balance of account {string} is {double}")
    public void the_new_balance_of_account_is(String iban, double newBalance) {
        Account updated = accountRepository.findById(iban)
                .orElseThrow(() -> new AssertionError("Account not found: " + iban));
        BigDecimal expected = BigDecimal.valueOf(newBalance).setScale(2, RoundingMode.HALF_UP);
        assertEquals(0, expected.compareTo(updated.getBalance()), "Balance mismatch");
    }

    @Then("the response contains the error message {string}")
    public void the_response_contains_the_error_message(String message) throws Exception {
        lastResult.andExpect(content().string(containsString(message)));
    }
}
