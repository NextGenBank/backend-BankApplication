package com.nextgenbank.backend.steps;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgenbank.backend.model.Account;
import com.nextgenbank.backend.model.AccountType;
import com.nextgenbank.backend.model.User;
import com.nextgenbank.backend.model.UserRole;
import com.nextgenbank.backend.model.UserStatus;
import com.nextgenbank.backend.model.dto.ApprovalRequestDto;
import com.nextgenbank.backend.model.dto.TransferRequestDto;
import com.nextgenbank.backend.repository.AccountRepository;
import com.nextgenbank.backend.repository.TransactionRepository;
import com.nextgenbank.backend.repository.UserRepository;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class EmployeeTransferSteps {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    private String authToken;
    private ResponseEntity<String> response;
    private String transferErrorMessage;
    
    // Add login method to be used in the setup
    private void loginAsEmployee() throws JsonProcessingException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // Use Bob's credentials from the DataInitializer
        String loginRequest = "{\"email\":\"bob@example.com\", \"password\":\"bob123\"}";
        
        HttpEntity<String> request = new HttpEntity<>(loginRequest, headers);
        response = restTemplate.exchange("/auth/login", HttpMethod.POST, request, String.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode(), "Login failed: " + response.getBody());
        
        JsonNode root = objectMapper.readTree(response.getBody());
        authToken = root.path("token").asText();
    }

    @Given("a customer with source account {string} and balance {double}")
    public void aCustomerWithSourceAccountAndBalance(String iban, double balance) {
        setupAccount(iban, balance, null);
    }

    @Given("a customer with destination account {string} and balance {double}")
    public void aCustomerWithDestinationAccountAndBalance(String iban, double balance) {
        setupAccount(iban, balance, null);
    }

    @Given("a customer with source account {string} and balance {double} and transfer limit {double}")
    public void aCustomerWithSourceAccountAndBalanceAndTransferLimit(String iban, double balance, double limit) {
        setupAccount(iban, balance, new BigDecimal(String.valueOf(limit)));
    }

    private void setupAccount(String iban, double balance, BigDecimal transferLimit) {
        // Check if the account already exists
        Optional<Account> existingAccount = accountRepository.findById(iban);
        
        if (existingAccount.isPresent()) {
            Account account = existingAccount.get();
            account.setBalance(new BigDecimal(String.valueOf(balance)));
            if (transferLimit != null) {
                account.setAbsoluteTransferLimit(transferLimit);
            }
            accountRepository.save(account);
            return;
        }
        
        // Create a new customer for the account if it doesn't exist
        User customer = new User();
        customer.setFirstName("Test");
        customer.setLastName("Customer");
        customer.setEmail("customer_" + iban + "@example.com");
        customer.setRole(UserRole.CUSTOMER);
        customer.setStatus(UserStatus.APPROVED);
        User savedCustomer = userRepository.save(customer);
        
        // Create the account
        Account account = new Account();
        account.setIBAN(iban);
        account.setCustomer(savedCustomer);
        account.setAccountType(AccountType.CHECKING);
        account.setBalance(new BigDecimal(String.valueOf(balance)));
        
        if (transferLimit != null) {
            account.setAbsoluteTransferLimit(transferLimit);
        } else {
            account.setAbsoluteTransferLimit(new BigDecimal("5000.00"));
        }
        
        account.setDailyTransferAmount(BigDecimal.ZERO);
        account.setCreatedAt(java.time.LocalDateTime.now());
        
        accountRepository.save(account);
    }

    @When("the employee transfers {double} from {string} to {string}")
    public void theEmployeeTransfersFromTo(double amount, String fromAccount, String toAccount) {
        // Make sure we're logged in
        if (authToken == null || authToken.isEmpty()) {
            try {
                loginAsEmployee();
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to login as employee", e);
            }
        }
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("accountNumber", fromAccount);
        requestBody.put("toAccount", toAccount);
        requestBody.put("amount", amount);
        requestBody.put("description", "Test transfer");
        
        // Get the initiator ID (employee) - Bob from DataInitializer
        User employee = userRepository.findByEmail("bob@example.com")
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        requestBody.put("initiatorId", employee.getUserId());
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        
        response = restTemplate.exchange(
                "/api/employees/transfer",
                HttpMethod.POST,
                request,
                String.class
        );
        
        assertEquals(HttpStatus.OK, response.getStatusCode(), 
                "Transfer should be successful. Response: " + response.getBody());
    }

    @When("the employee attempts to transfer {double} from {string} to {string}")
    public void theEmployeeAttemptsToTransferFromTo(double amount, String fromAccount, String toAccount) {
        // Make sure we're logged in
        if (authToken == null || authToken.isEmpty()) {
            try {
                loginAsEmployee();
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to login as employee", e);
            }
        }
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("accountNumber", fromAccount);
        requestBody.put("toAccount", toAccount);
        requestBody.put("amount", amount);
        requestBody.put("description", "Test transfer");
        
        // Get the initiator ID (employee) - Bob from DataInitializer
        User employee = userRepository.findByEmail("bob@example.com")
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        requestBody.put("initiatorId", employee.getUserId());
        
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        
        response = restTemplate.exchange(
                "/api/employees/transfer",
                HttpMethod.POST,
                request,
                String.class
        );
        
        if (response.getStatusCode() != HttpStatus.OK) {
            try {
                JsonNode errorResponse = objectMapper.readTree(response.getBody());
                if (errorResponse.has("message")) {
                    transferErrorMessage = errorResponse.get("message").asText();
                } else if (errorResponse.has("error")) {
                    transferErrorMessage = errorResponse.get("error").asText();
                } else {
                    transferErrorMessage = "Unknown error";
                }
            } catch (Exception e) {
                transferErrorMessage = response.getBody();
            }
        }
    }

    @Then("the source account {string} should have balance {double}")
    public void theSourceAccountShouldHaveBalance(String iban, double expectedBalance) {
        Account account = accountRepository.findById(iban)
                .orElseThrow(() -> new RuntimeException("Account not found: " + iban));
                
        BigDecimal actual = account.getBalance();
        BigDecimal expected = new BigDecimal(String.valueOf(expectedBalance));
        
        assertEquals(0, expected.compareTo(actual), 
                "Expected balance " + expected + " but was " + actual);
    }

    @Then("the destination account {string} should have balance {double}")
    public void theDestinationAccountShouldHaveBalance(String iban, double expectedBalance) {
        Account account = accountRepository.findById(iban)
                .orElseThrow(() -> new RuntimeException("Account not found: " + iban));
                
        BigDecimal actual = account.getBalance();
        BigDecimal expected = new BigDecimal(String.valueOf(expectedBalance));
        
        assertEquals(0, expected.compareTo(actual), 
                "Expected balance " + expected + " but was " + actual);
    }

    @Then("a transaction record should be created")
    public void aTransactionRecordShouldBeCreated() {
        long transactionCount = transactionRepository.count();
        assertTrue(transactionCount > 0, "At least one transaction should exist");
    }

    @Then("the transfer should be rejected with message containing {string}")
    public void theTransferShouldBeRejectedWithMessageContaining(String expectedMessagePart) {
        assertNotEquals(HttpStatus.OK, response.getStatusCode(), 
                "Transfer should have been rejected");
                
        assertNotNull(transferErrorMessage, "Error message should not be null");
        assertTrue(transferErrorMessage.contains(expectedMessagePart), 
                "Error message should contain '" + expectedMessagePart + "' but was: " + transferErrorMessage);
    }

    @When("the employee updates the transfer limit for account {string} to {double}")
    public void theEmployeeUpdatesTheTransferLimitForAccountTo(String iban, double limit) {
        // Make sure we're logged in
        if (authToken == null || authToken.isEmpty()) {
            try {
                loginAsEmployee();
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to login as employee", e);
            }
        }
        
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        ApprovalRequestDto requestDto = new ApprovalRequestDto();
        requestDto.setAccountIban(iban);
        requestDto.setAbsoluteTransferLimit(new BigDecimal(String.valueOf(limit)));
        
        HttpEntity<ApprovalRequestDto> request = new HttpEntity<>(requestDto, headers);
        
        response = restTemplate.exchange(
                "/api/accounts/limit",
                HttpMethod.PUT,
                request,
                String.class
        );
        
        assertEquals(HttpStatus.OK, response.getStatusCode(), 
                "Limit update should be successful. Response: " + response.getBody());
    }

    @Then("the account {string} should have a transfer limit of {double}")
    public void theAccountShouldHaveATransferLimitOf(String iban, double expectedLimit) {
        Account account = accountRepository.findById(iban)
                .orElseThrow(() -> new RuntimeException("Account not found: " + iban));
                
        BigDecimal actual = account.getAbsoluteTransferLimit();
        BigDecimal expected = new BigDecimal(String.valueOf(expectedLimit));
        
        assertEquals(0, expected.compareTo(actual), 
                "Expected transfer limit " + expected + " but was " + actual);
    }
}