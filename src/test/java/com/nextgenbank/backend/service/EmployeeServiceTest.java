package com.nextgenbank.backend.service;

import com.nextgenbank.backend.model.User;
import com.nextgenbank.backend.model.UserStatus;
import com.nextgenbank.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class EmployeeServiceTest {

    private UserRepository userRepository;
    private EmployeeService employeeService;
    private AccountService accountService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        accountService = mock(AccountService.class);
        employeeService = new EmployeeService(userRepository, accountService);
    }

    @Test
    void shouldApproveCustomerAndCreateAccounts() {
        User customer = new User();
        customer.setUserId(1L);
        customer.setStatus(UserStatus.PENDING);

        User employee = new User();
        employee.setUserId(2L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(customer));

        employeeService.approveCustomer(1L, employee);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals(UserStatus.APPROVED, userCaptor.getValue().getStatus());

        verify(accountService).createAccountsForUser(customer, employee);
    }

    @Test
    void shouldRejectCustomer() {
        User customer = new User();
        customer.setUserId(1L);
        customer.setStatus(UserStatus.PENDING);

        when(userRepository.findById(1L)).thenReturn(Optional.of(customer));

        employeeService.rejectCustomer(1L);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals(UserStatus.REJECTED, userCaptor.getValue().getStatus());
    }
}
