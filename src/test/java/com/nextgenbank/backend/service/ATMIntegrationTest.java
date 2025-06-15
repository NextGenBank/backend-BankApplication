package com.nextgenbank.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nextgenbank.backend.model.Account;
import com.nextgenbank.backend.model.AccountType;
import com.nextgenbank.backend.model.TransactionType;
import com.nextgenbank.backend.model.User;
import com.nextgenbank.backend.model.UserRole;
import com.nextgenbank.backend.model.UserStatus;
import com.nextgenbank.backend.model.dto.TransactionDto;
import com.nextgenbank.backend.repository.AccountRepository;
import com.nextgenbank.backend.repository.TransactionRepository;
import com.nextgenbank.backend.repository.UserRepository;
import com.nextgenbank.backend.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ATMIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository; // Added for complete cleanup

    @Autowired
    private PasswordEncoder passwordEncoder;

    private User testUser;
    private Account testAccount;

    @BeforeEach
    void setUp() {
        // Correct cleanup order: delete entities with foreign keys first.
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();

        // Create and save a test user
        testUser = new User();
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setEmail("john.doe@example.com");
        testUser.setPassword(passwordEncoder.encode("password123"));
        testUser.setRole(UserRole.CUSTOMER);
        testUser.setStatus(UserStatus.APPROVED);
        testUser.setBsnNumber("123456789");
        testUser.setPhoneNumber("1234567890");
        testUser.setCreatedAt(LocalDateTime.now());
        testUser = userRepository.save(testUser);

        // Create and save an account for the test user
        testAccount = new Account();
        testAccount.setIBAN("NL01NEXT0000000001");
        testAccount.setCustomer(testUser);
        testAccount.setAccountType(AccountType.CHECKING);
        testAccount.setBalance(new BigDecimal("1000.00"));
        testAccount.setCreatedAt(LocalDateTime.now());
        accountRepository.save(testAccount);
    }

    @Test
    void createAtmTransaction_Deposit_Success() throws Exception {
        // Prepare request body
        TransactionDto depositDto = new TransactionDto();
        depositDto.setTransactionType(TransactionType.DEPOSIT);
        depositDto.setToIban(testAccount.getIBAN());
        depositDto.setAmount(new BigDecimal("250.50"));

        // Perform POST request with an authenticated user
        mockMvc.perform(post("/api/transactions/atm")
                        .with(user(new UserPrincipal(testUser))) // Simulate an authenticated user
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(depositDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionType", is("DEPOSIT")))
                .andExpect(jsonPath("$.amount", is(250.50)))
                .andExpect(jsonPath("$.toIban", is(testAccount.getIBAN())));

        // Verify the balance was updated in the database
        Account updatedAccount = accountRepository.findById(testAccount.getIBAN()).orElseThrow();
        assertEquals(0, new BigDecimal("1250.50").compareTo(updatedAccount.getBalance()));
    }

    @Test
    void createAtmTransaction_Withdrawal_Success() throws Exception {
        // Prepare request body
        TransactionDto withdrawDto = new TransactionDto();
        withdrawDto.setTransactionType(TransactionType.WITHDRAWAL);
        withdrawDto.setFromIban(testAccount.getIBAN());
        withdrawDto.setAmount(new BigDecimal("100.00"));

        // Perform POST request
        mockMvc.perform(post("/api/transactions/atm")
                        .with(user(new UserPrincipal(testUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(withdrawDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionType", is("WITHDRAWAL")))
                .andExpect(jsonPath("$.amount", is(100.00)))
                .andExpect(jsonPath("$.fromIban", is(testAccount.getIBAN())));

        // Verify the balance was updated
        Account updatedAccount = accountRepository.findById(testAccount.getIBAN()).orElseThrow();
        assertEquals(0, new BigDecimal("900.00").compareTo(updatedAccount.getBalance()));
    }

    @Test
    void createAtmTransaction_Withdrawal_Fails_InsufficientFunds() throws Exception {
        TransactionDto withdrawDto = new TransactionDto();
        withdrawDto.setTransactionType(TransactionType.WITHDRAWAL);
        withdrawDto.setFromIban(testAccount.getIBAN());
        withdrawDto.setAmount(new BigDecimal("2000.00")); // More than the balance

        mockMvc.perform(post("/api/transactions/atm")
                        .with(user(new UserPrincipal(testUser)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(withdrawDto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Insufficient funds."));

        // Verify balance did not change
        Account notUpdatedAccount = accountRepository.findById(testAccount.getIBAN()).orElseThrow();
        assertEquals(0, new BigDecimal("1000.00").compareTo(notUpdatedAccount.getBalance()));
    }

    @Test
    void createAtmTransaction_Fails_Unauthenticated() throws Exception {
        // This test proves why you get a 401 error. No user is attached to the request.
        TransactionDto depositDto = new TransactionDto();
        depositDto.setTransactionType(TransactionType.DEPOSIT);
        depositDto.setToIban(testAccount.getIBAN());
        depositDto.setAmount(new BigDecimal("100.00"));

        mockMvc.perform(post("/api/transactions/atm")
                        // Notice the missing .with(user(...)) here
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(depositDto)))
                .andExpect(status().isUnauthorized()); // Expect 401 Unauthorized
    }

    @Test
    void createAtmTransaction_Fails_AccessDenied() throws Exception {
        // Create another user
        User anotherUser = new User();
        anotherUser.setFirstName("Jane");
        anotherUser.setLastName("Doe");
        anotherUser.setEmail("jane.doe@example.com");
        anotherUser.setPassword(passwordEncoder.encode("password123"));
        anotherUser.setRole(UserRole.CUSTOMER);
        anotherUser.setStatus(UserStatus.APPROVED);
        anotherUser.setBsnNumber("987654321");
        anotherUser.setPhoneNumber("0987654321");
        anotherUser.setCreatedAt(LocalDateTime.now());
        userRepository.save(anotherUser);

        // Try to withdraw from testUser's account while authenticated as anotherUser
        TransactionDto withdrawDto = new TransactionDto();
        withdrawDto.setTransactionType(TransactionType.WITHDRAWAL);
        withdrawDto.setFromIban(testAccount.getIBAN()); // John's account
        withdrawDto.setAmount(new BigDecimal("50.00"));

        mockMvc.perform(post("/api/transactions/atm")
                        .with(user(new UserPrincipal(anotherUser))) // Authenticated as Jane
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(withdrawDto)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Access denied to this account."));
    }
}
