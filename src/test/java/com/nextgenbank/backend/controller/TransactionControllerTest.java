package com.nextgenbank.backend.controller;

import com.nextgenbank.backend.model.Account;
import com.nextgenbank.backend.model.Transaction;
import com.nextgenbank.backend.model.TransactionType;
import com.nextgenbank.backend.model.User;
import com.nextgenbank.backend.model.dto.TransactionDto;
import com.nextgenbank.backend.model.dto.TransactionResponseDto;
import com.nextgenbank.backend.security.UserPrincipal;
import com.nextgenbank.backend.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit‐тесты для методов createDeposit и createWithdraw в TransactionController.
 * Вызываем методы контроллера напрямую, без MockMvc и Spring Security фильтров.
 */
class TransactionControllerTest {

    @Mock
    private TransactionService transactionService;

    private TransactionController controller;

    private User testUser;
    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        controller = new TransactionController(transactionService);

        // Создаём «тестового» пользователя
        testUser = new User();
        testUser.setUserId(42L);
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setEmail("test@example.com");
        // Роль и статус можно опустить, они не препятствуют тесту контроллера

        // Через конструктор: UserPrincipal оборачивает User
        userPrincipal = new UserPrincipal(testUser);
    }

    //
    // ТЕСТЫ ДЛЯ createDeposit
    //

    @Test
    void createDeposit_whenDtoInvalid_shouldReturnBadRequest() {
        // DTO без toIban и amount
        TransactionDto dto = new TransactionDto();
        dto.setToIban(null);
        dto.setAmount(null);

        ResponseEntity<?> response = controller.createDeposit(dto, userPrincipal);

        assertEquals(400, response.getStatusCodeValue());
        assertEquals("toIban and amount are required", response.getBody());
        // Убедимся, что сервис не вызывался
        verify(transactionService, never()).doDeposit(any(), anyString(), any());
    }

    @Test
    void createDeposit_whenServiceThrowsException_shouldReturnBadRequestWithMessage() {
        // DTO с валидными полями
        TransactionDto dto = new TransactionDto();
        dto.setToIban("NL123");
        dto.setAmount(new BigDecimal("100"));

        // Мокаем сервис, чтобы он выбросил ошибку
        doThrow(new IllegalArgumentException("Access denied"))
                .when(transactionService)
                .doDeposit(eq(testUser), eq("NL123"), eq(new BigDecimal("100")));

        ResponseEntity<?> response = controller.createDeposit(dto, userPrincipal);

        assertEquals(400, response.getStatusCodeValue());
        assertEquals("Access denied", response.getBody());
        verify(transactionService, times(1))
                .doDeposit(eq(testUser), eq("NL123"), eq(new BigDecimal("100")));
    }

    @Test
    void createDeposit_whenEverythingIsOk_shouldReturnFullResponseDto() {
        // DTO для депозита
        TransactionDto dto = new TransactionDto();
        dto.setToIban("NL999");
        dto.setAmount(new BigDecimal("250"));

        // Счёт получателя с привязкой к testUser
        Account toAcc = new Account();
        toAcc.setIBAN("NL999");
        toAcc.setCustomer(testUser);

        // Настраиваем Transaction, который вернёт сервис
        Transaction tx = new Transaction();
        tx.setTransactionId(20L);
        tx.setFromAccount(null);
        tx.setToAccount(toAcc);
        tx.setAmount(new BigDecimal("250"));
        tx.setTransactionType(TransactionType.DEPOSIT);
        tx.setTimestamp(LocalDateTime.of(2025, 6, 5, 15, 0));
        tx.setInitiator(testUser);

        // Мокаем сервис, чтобы вернуть созданную транзакцию
        when(transactionService.doDeposit(eq(testUser), eq("NL999"), eq(new BigDecimal("250"))))
                .thenReturn(tx);

        ResponseEntity<?> response = controller.createDeposit(dto, userPrincipal);

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody() instanceof TransactionResponseDto);

        TransactionResponseDto resultDto = (TransactionResponseDto) response.getBody();
        // Проверяем все поля в DTO
        assertEquals(20L, resultDto.transactionId());
        assertEquals(TransactionType.DEPOSIT, resultDto.transactionType());
        assertEquals(new BigDecimal("250"), resultDto.amount());
        assertEquals(LocalDateTime.of(2025, 6, 5, 15, 0), resultDto.timestamp());
        assertNull(resultDto.fromIban(),  "DEPOSIT → fromIban должно быть null");
        assertNull(resultDto.fromName(),  "DEPOSIT → fromName должно быть null");
        assertEquals("NL999", resultDto.toIban());
        assertEquals("Test User", resultDto.toName());
        assertEquals("DEPOSIT", resultDto.direction());

        verify(transactionService, times(1))
                .doDeposit(eq(testUser), eq("NL999"), eq(new BigDecimal("250")));
    }

    //
    // ТЕСТЫ ДЛЯ createWithdraw
    //

    @Test
    void createWithdraw_whenDtoInvalid_shouldReturnBadRequest() {
        // DTO без fromIban и amount
        TransactionDto dto = new TransactionDto();
        dto.setFromIban(null);
        dto.setAmount(null);
        dto.setBills(null);

        ResponseEntity<?> response = controller.createWithdraw(dto, userPrincipal);

        assertEquals(400, response.getStatusCodeValue());
        assertEquals("fromIban and amount are required", response.getBody());
        verify(transactionService, never()).doWithdraw(any(), anyString(), any(), any());
    }

    @Test
    void createWithdraw_whenServiceThrowsException_shouldReturnBadRequestWithMessage() {
        // DTO с валидными полями
        TransactionDto dto = new TransactionDto();
        dto.setFromIban("NLABC");
        dto.setAmount(new BigDecimal("75"));
        dto.setBills(10);

        // Мокаем сервис, чтобы он выбросил ошибку
        doThrow(new IllegalArgumentException("Insufficient funds"))
                .when(transactionService)
                .doWithdraw(eq(testUser), eq("NLABC"), eq(new BigDecimal("75")), eq(10));

        ResponseEntity<?> response = controller.createWithdraw(dto, userPrincipal);

        assertEquals(400, response.getStatusCodeValue());
        assertEquals("Insufficient funds", response.getBody());
        verify(transactionService, times(1))
                .doWithdraw(eq(testUser), eq("NLABC"), eq(new BigDecimal("75")), eq(10));
    }

    @Test
    void createWithdraw_whenEverythingIsOk_shouldReturnFullResponseDto() {
        // DTO для вывода
        TransactionDto dto = new TransactionDto();
        dto.setFromIban("NLXYZ");
        dto.setAmount(new BigDecimal("60"));
        dto.setBills(5);

        // Счёт отправителя с привязкой к testUser
        Account fromAcc = new Account();
        fromAcc.setIBAN("NLXYZ");
        fromAcc.setCustomer(testUser);

        // Настраиваем Transaction, который вернёт сервис
        Transaction tx = new Transaction();
        tx.setTransactionId(30L);
        tx.setFromAccount(fromAcc);
        tx.setToAccount(null);
        tx.setAmount(new BigDecimal("60"));
        tx.setTransactionType(TransactionType.WITHDRAWAL);
        tx.setTimestamp(LocalDateTime.of(2025, 6, 5, 16, 0));
        tx.setInitiator(testUser);

        // Мокаем сервис, чтобы вернуть созданную транзакцию
        when(transactionService.doWithdraw(eq(testUser), eq("NLXYZ"), eq(new BigDecimal("60")), eq(5)))
                .thenReturn(tx);

        ResponseEntity<?> response = controller.createWithdraw(dto, userPrincipal);

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getBody() instanceof TransactionResponseDto);

        TransactionResponseDto resultDto = (TransactionResponseDto) response.getBody();
        // Проверяем все поля в DTO
        assertEquals(30L, resultDto.transactionId());
        assertEquals(TransactionType.WITHDRAWAL, resultDto.transactionType());
        assertEquals(new BigDecimal("60"), resultDto.amount());
        assertEquals(LocalDateTime.of(2025, 6, 5, 16, 0), resultDto.timestamp());
        assertEquals("NLXYZ", resultDto.fromIban());
        assertEquals("Test User", resultDto.fromName());
        assertNull(resultDto.toIban(), "WITHDRAWAL → toIban должно быть null");
        assertNull(resultDto.toName(), "WITHDRAWAL → toName должно быть null");
        assertEquals("WITHDRAWAL", resultDto.direction());

        verify(transactionService, times(1))
                .doWithdraw(eq(testUser), eq("NLXYZ"), eq(new BigDecimal("60")), eq(5));
    }
}
