// src/test/java/com/nextgenbank/backend/controller/ATMControllerTest.java
package com.nextgenbank.backend.controller;

import com.nextgenbank.backend.model.Account;
import com.nextgenbank.backend.model.Transaction;
import com.nextgenbank.backend.model.TransactionType;
import com.nextgenbank.backend.model.User;
import com.nextgenbank.backend.security.UserPrincipal;
import com.nextgenbank.backend.service.ATMService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ATMController.class)
class ATMControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ATMService atmService;

    private void setupSecurityContext(User user) {
        UserPrincipal principal = new UserPrincipal(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void createDeposit_Success() throws Exception {
        // Arrange
        User user = new User();
        user.setUserId(1L);
        user.setFirstName("John");
        user.setLastName("Doe");
        setupSecurityContext(user);

        Account account = new Account();
        account.setIBAN("NL1234567890");
        account.setCustomer(user);

        Transaction transaction = new Transaction();
        transaction.setTransactionId(1L);
        transaction.setTransactionType(TransactionType.DEPOSIT);
        transaction.setAmount(BigDecimal.valueOf(500));
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setToAccount(account);
        transaction.setInitiator(user);

        when(atmService.doDeposit(any(), anyString(), any())).thenReturn(transaction);

        // Act & Assert
        mockMvc.perform(post("/api/atm/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toIban\":\"NL1234567890\",\"amount\":500}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value(1))
                .andExpect(jsonPath("$.toIban").value("NL1234567890"))
                .andExpect(jsonPath("$.toName").value("John Doe"))
                .andExpect(jsonPath("$.direction").value("DEPOSIT"));
    }

    @Test
    void createDeposit_MissingFields() throws Exception {
        // Arrange
        User user = new User();
        setupSecurityContext(user);

        // Act & Assert
        mockMvc.perform(post("/api/atm/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$").value("toIban and amount are required"));
    }

    @Test
    void createWithdraw_Success() throws Exception {
        // Arrange
        User user = new User();
        user.setUserId(1L);
        user.setFirstName("John");
        user.setLastName("Doe");
        setupSecurityContext(user);

        Account account = new Account();
        account.setIBAN("NL1234567890");
        account.setCustomer(user);

        Transaction transaction = new Transaction();
        transaction.setTransactionId(1L);
        transaction.setTransactionType(TransactionType.WITHDRAWAL);
        transaction.setAmount(BigDecimal.valueOf(200));
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setFromAccount(account);
        transaction.setInitiator(user);

        when(atmService.doWithdraw(any(), anyString(), any(), any())).thenReturn(transaction);

        // Act & Assert
        mockMvc.perform(post("/api/atm/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fromIban\":\"NL1234567890\",\"amount\":200}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionId").value(1))
                .andExpect(jsonPath("$.fromIban").value("NL1234567890"))
                .andExpect(jsonPath("$.fromName").value("John Doe"))
                .andExpect(jsonPath("$.direction").value("WITHDRAWAL"));
    }

    @Test
    void createWithdraw_MissingFields() throws Exception {
        // Arrange
        User user = new User();
        setupSecurityContext(user);

        // Act & Assert
        mockMvc.perform(post("/api/atm/withdraw")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$").value("fromIban and amount are required"));
    }
}