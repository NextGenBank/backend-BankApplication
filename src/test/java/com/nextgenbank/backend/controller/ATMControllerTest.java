package com.nextgenbank.backend.controller;

import com.nextgenbank.backend.model.Account;
import com.nextgenbank.backend.model.Transaction;
import com.nextgenbank.backend.model.TransactionType;
import com.nextgenbank.backend.model.User;
import com.nextgenbank.backend.model.dto.TransactionDto;
import com.nextgenbank.backend.model.dto.TransactionResponseDto;
import com.nextgenbank.backend.security.UserPrincipal;
import com.nextgenbank.backend.service.ATMService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

class ATMControllerTest {

    private ATMService atmService;
    private ATMController controller;

    @BeforeEach
    void setUp() {
        atmService = mock(ATMService.class);
        controller = new ATMController(atmService);
    }

    private UserPrincipal principal() {
        User u = new User();
        u.setUserId(1L);
        u.setFirstName("Ivan");
        u.setLastName("Petrov");
        u.setRole(com.nextgenbank.backend.model.UserRole.CUSTOMER);
        u.setStatus(com.nextgenbank.backend.model.UserStatus.APPROVED);
        return new UserPrincipal(u);
    }

    @Test
    @DisplayName("deposit(): 400 if it's not toIban or amount")
    void deposit_missingFields() {
        TransactionDto dto = new TransactionDto();
        // и toIban и amount == null
        ResponseEntity<?> resp = controller.createDeposit(dto, principal());
        assertEquals(400, resp.getStatusCodeValue());
        assertEquals("toIban and amount are required", resp.getBody());
    }

    @Test
    @DisplayName("deposit(): 200 & right DTO during the success")
    void deposit_success() {
        // подготовка входных данных
        TransactionDto dto = new TransactionDto();
        dto.setToIban("DE123");
        dto.setAmount(new BigDecimal("100.00"));

        // договоримся, что сервис вернёт вот такую транзакцию
        Transaction tx = new Transaction();
        tx.setTransactionId(42L);
        tx.setTransactionType(TransactionType.DEPOSIT);
        tx.setAmount(dto.getAmount());
        tx.setTimestamp(LocalDateTime.of(2025, 6, 9, 12, 0));
        Account acc = new Account();
        acc.setIBAN("DE123");
        User cust = new User();
        cust.setUserId(1L);
        cust.setFirstName("Ivan");
        cust.setLastName("Petrov");
        acc.setCustomer(cust);
        tx.setToAccount(acc);
        tx.setInitiator(principal().getUser());

        given(atmService.doDeposit(
                any(User.class),
                eq("DE123"),
                eq(new BigDecimal("100.00"))
        ))
                .willReturn(tx);

        // вызываем контроллер
        ResponseEntity<?> resp = controller.createDeposit(dto, principal());
        assertEquals(200, resp.getStatusCodeValue());

        TransactionResponseDto body = (TransactionResponseDto) resp.getBody();
        assertNotNull(body);
        assertEquals(42L, body.transactionId());
        assertEquals(TransactionType.DEPOSIT, body.transactionType());
        assertEquals(new BigDecimal("100.00"), body.amount());
        assertEquals("DE123", body.toIban());
        assertEquals("Ivan Petrov", body.toName());
        assertEquals("DEPOSIT", body.direction());
    }

    @Test
    @DisplayName("deposit(): 400 during AccessDenied from Service")
    void deposit_accessDenied() {
        TransactionDto dto = new TransactionDto();
        dto.setToIban("XYZ");
        dto.setAmount(new BigDecimal("50"));
        given(atmService.doDeposit(any(), anyString(), any()))
                .willThrow(new IllegalArgumentException("Access denied to deposit into this account"));
    }
}