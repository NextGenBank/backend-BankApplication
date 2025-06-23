package com.nextgenbank.backend.service;

import com.nextgenbank.backend.model.User;
import com.nextgenbank.backend.model.UserRole;
import com.nextgenbank.backend.model.UserStatus;
import com.nextgenbank.backend.model.dto.UserDto;
import com.nextgenbank.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


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
    void shouldReturnAllCustomers() {
        // Given
        User customer1 = createCustomer(1L, "John", "Doe", UserStatus.APPROVED);
        User customer2 = createCustomer(2L, "Jane", "Smith", UserStatus.PENDING);
        when(userRepository.findByRole(UserRole.CUSTOMER)).thenReturn(Arrays.asList(customer1, customer2));

        // When
        List<UserDto> result = employeeService.getAllCustomers();

        // Then
        assertEquals(2, result.size());
        assertEquals("John", result.get(0).getFirstName());
        assertEquals("Jane", result.get(1).getFirstName());
    }

    @Test
    void shouldReturnAllCustomersPaginated() {
        // Given
        User customer1 = createCustomer(1L, "John", "Doe", UserStatus.APPROVED);
        User customer2 = createCustomer(2L, "Jane", "Smith", UserStatus.PENDING);
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> customerPage = new PageImpl<>(Arrays.asList(customer1, customer2));
        
        when(userRepository.findByRole(UserRole.CUSTOMER, pageable)).thenReturn(customerPage);

        // When
        Page<UserDto> result = employeeService.getAllCustomersPaginated(pageable);

        // Then
        assertEquals(2, result.getTotalElements());
        assertEquals("John", result.getContent().get(0).getFirstName());
        assertEquals("Jane", result.getContent().get(1).getFirstName());
    }

    @Test
    void shouldReturnCustomersByStatusPaginated() {
        // Given
        User customer1 = createCustomer(1L, "John", "Doe", UserStatus.APPROVED);
        User customer2 = createCustomer(2L, "Jane", "Smith", UserStatus.APPROVED);
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> customerPage = new PageImpl<>(Arrays.asList(customer1, customer2));
        
        when(userRepository.findByRoleAndStatus(UserRole.CUSTOMER, UserStatus.APPROVED, pageable))
            .thenReturn(customerPage);

        // When
        Page<UserDto> result = employeeService.getCustomersByStatusPaginated(UserStatus.APPROVED, pageable);

        // Then
        assertEquals(2, result.getTotalElements());
        assertEquals("John", result.getContent().get(0).getFirstName());
        assertEquals("Jane", result.getContent().get(1).getFirstName());
        assertEquals(UserStatus.APPROVED, result.getContent().get(0).getStatus());
    }

    @Test
    void shouldReturnCustomerById() {
        // Given
        Long customerId = 1L;
        User customer = createCustomer(customerId, "John", "Doe", UserStatus.APPROVED);
        when(userRepository.findById(customerId)).thenReturn(Optional.of(customer));

        // When
        UserDto result = employeeService.getCustomerById(customerId);

        // Then
        assertEquals(customerId, result.getUserId());
        assertEquals("John", result.getFirstName());
        assertEquals("Doe", result.getLastName());
        assertEquals(UserStatus.APPROVED, result.getStatus());
    }

    @Test
    void shouldThrowExceptionWhenCustomerNotFound() {
        // Given
        Long customerId = 1L;
        when(userRepository.findById(customerId)).thenReturn(Optional.empty());

        // When & Then
        Exception exception = assertThrows(RuntimeException.class,
                () -> employeeService.getCustomerById(customerId));
        assertEquals("Customer not found", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenUserIsNotCustomer() {
        // Given
        Long userId = 1L;
        User employee = new User();
        employee.setUserId(userId);
        employee.setFirstName("John");
        employee.setLastName("Doe");
        employee.setRole(UserRole.EMPLOYEE);
        employee.setStatus(UserStatus.APPROVED);
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(employee));

        // When & Then
        Exception exception = assertThrows(RuntimeException.class,
                () -> employeeService.getCustomerById(userId));
        assertEquals("User is not a customer", exception.getMessage());
    }

    @Test
    void shouldApproveCustomer() {
        // Given
        Long customerId = 1L;
        User customer = createCustomer(customerId, "John", "Doe", UserStatus.PENDING);
        when(userRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        UserDto result = employeeService.approveCustomer(customerId);
        
        // Then
        assertEquals(UserStatus.APPROVED, result.getStatus());
        verify(accountService).createAccountsForUser(any(User.class));
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
        // Given
        Long customerId = 1L;
        User customer = createCustomer(customerId, "John", "Doe", UserStatus.PENDING);
        when(userRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        
        // When
        UserDto result = employeeService.rejectCustomer(customerId);
        
        // Then
        assertEquals(UserStatus.REJECTED, result.getStatus());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldThrowExceptionWhenApprovingAlreadyApprovedCustomer() {
        // Given
        Long customerId = 1L;
        User customer = createCustomer(customerId, "John", "Doe", UserStatus.APPROVED);
        when(userRepository.findById(customerId)).thenReturn(Optional.of(customer));
        
        // When & Then
        Exception exception = assertThrows(IllegalStateException.class,
                () -> employeeService.approveCustomer(customerId));
        assertEquals("User is already approved.", exception.getMessage());
    }

    @Test
    void shouldThrowExceptionWhenRejectingAlreadyRejectedCustomer() {
        // Given
        Long customerId = 1L;
        User customer = createCustomer(customerId, "John", "Doe", UserStatus.REJECTED);
        when(userRepository.findById(customerId)).thenReturn(Optional.of(customer));
        
        // When & Then
        Exception exception = assertThrows(IllegalStateException.class,
                () -> employeeService.rejectCustomer(customerId));
        assertEquals("User is already rejected.", exception.getMessage());
    }

    private User createCustomer(Long id, String firstName, String lastName, UserStatus status) {
        User customer = new User();
        customer.setUserId(id);
        customer.setFirstName(firstName);
        customer.setLastName(lastName);
        customer.setRole(UserRole.CUSTOMER);
        customer.setStatus(status);
        customer.setEmail(firstName.toLowerCase() + "." + lastName.toLowerCase() + "@example.com");
        return customer;
    }

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
