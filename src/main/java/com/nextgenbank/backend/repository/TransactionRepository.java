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
    List<Transaction> findByFromAccountOrToAccountOrderByTimestampDesc(Account fromAccount, Account toAccount);
    List<Transaction> findAllByOrderByTimestampDesc();
    List<Transaction> findByInitiatorOrderByTimestampDesc(User initiator);
    List<Transaction> findByFromAccount_CustomerOrToAccount_CustomerOrderByTimestampDesc(User customer, User sameCustomer);

    @Query("""
       SELECT t
       FROM Transaction t
       LEFT JOIN t.fromAccount fa
       LEFT JOIN t.toAccount   ta
       WHERE (fa.customer.userId = :userId) 
          OR (ta.customer.userId = :userId)
    """)
    List<Transaction> findAllByUserId(@Param("userId") Long userId);
}
