package com.nextgenbank.backend.service;

import com.nextgenbank.backend.BackendApplication;
import com.nextgenbank.backend.model.*;
import com.nextgenbank.backend.model.dto.TransactionDto;
import com.nextgenbank.backend.repository.AccountRepository;
import com.nextgenbank.backend.repository.TransactionRepository;
import com.nextgenbank.backend.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = BackendApplication.class)
@AutoConfigureMockMvc(addFilters = false)   // ОТКЛЮЧАЕМ все Security-фильтры!
class ATMIntegrationTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper om;
    @Autowired
    private TransactionRepository transactionRepo;
    @Autowired
    private AccountRepository accountRepo;
    @Autowired
    private UserRepository userRepo;

    private User customer;
    private Account account;

    @BeforeEach
    void setUp() {
        // очистим всё
        transactionRepo.deleteAll();
        accountRepo.deleteAll();
        userRepo.deleteAll();

        // создаём пользователя
        customer = new User();
        customer.setFirstName("Ivan");
        customer.setLastName("Petrov");
        customer.setEmail("ivan.petrov@example.com");
        customer.setPassword("secret");
        customer.setRole(UserRole.CUSTOMER);
        customer.setStatus(UserStatus.APPROVED);
        customer = userRepo.save(customer);

        // создаём счёт с балансом 1000
        account = new Account();
        account.setIBAN("DE01_USER1");
        account.setAccountType(AccountType.CHECKING);
        account.setBalance(BigDecimal.valueOf(1000));
        account.setCustomer(customer);
        account = accountRepo.save(account);
    }

    @Test
    void depositEndpoint_returns200_andTypeDeposit() throws Exception {
        TransactionDto dto = new TransactionDto();
        dto.setTransactionType(TransactionType.DEPOSIT);
        dto.setToIban(account.getIBAN());
        dto.setAmount(BigDecimal.valueOf(500));

        mvc.perform(post("/api/transactions/atm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(dto))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionType").value("DEPOSIT"))
                .andExpect(jsonPath("$.amount").value(500.0));
    }

    @Test
    void withdrawEndpoint_succeeds() throws Exception {
        TransactionDto dto = new TransactionDto();
        dto.setTransactionType(TransactionType.WITHDRAWAL);
        dto.setFromIban(account.getIBAN());
        dto.setAmount(BigDecimal.valueOf(300));

        mvc.perform(post("/api/transactions/atm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(dto))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transactionType").value("WITHDRAWAL"))
                .andExpect(jsonPath("$.amount").value(300.0));
    }

    @Test
    void withdrawEndpoint_insufficientFunds_returns400() throws Exception {
        TransactionDto dto = new TransactionDto();
        dto.setTransactionType(TransactionType.WITHDRAWAL);
        dto.setFromIban(account.getIBAN());
        dto.setAmount(BigDecimal.valueOf(1500));

        mvc.perform(post("/api/transactions/atm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(dto))
                )
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Insufficient funds."));
    }
}
