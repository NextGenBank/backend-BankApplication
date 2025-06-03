package com.nextgenbank.backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;

    private String firstName;
    private String lastName;

    @Column(unique = true)
    private String email;

    private String password;
    private String bsnNumber;
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    private UserStatus status;

    @Column(columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    // Bidirectional mappings
    @OneToMany(mappedBy = "customer")
    @JsonIgnore
    private List<Account> accountsOwned = new ArrayList<>();

    @OneToMany(mappedBy = "createdBy")
    @JsonIgnore
    private List<Account> accountsCreated = new ArrayList<>();

    @OneToMany(mappedBy = "initiator")
    @JsonIgnore
    private List<Transaction> transactionsInitiated = new ArrayList<>();

    // Getters and Setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getBsnNumber() {
        return bsnNumber;
    }

    public void setBsnNumber(String bsnNumber) {
        this.bsnNumber = bsnNumber;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<Account> getAccountsOwned() {
        return accountsOwned;
    }

    public void setAccountsOwned(List<Account> accountsOwned) {
        this.accountsOwned = accountsOwned;
    }
}