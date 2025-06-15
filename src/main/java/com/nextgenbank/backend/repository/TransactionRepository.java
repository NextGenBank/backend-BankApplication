// src/main/java/com/nextgenbank/backend/repository/TransactionRepository.java
package com.nextgenbank.backend.repository;

import com.nextgenbank.backend.model.Account;
import com.nextgenbank.backend.model.Transaction;
import com.nextgenbank.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findAllByOrderByTimestampDesc();
    List<Transaction> findByFromAccount_CustomerOrToAccount_CustomerOrderByTimestampDesc(User customer, User sameCustomer);
}
