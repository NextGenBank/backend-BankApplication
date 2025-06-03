package com.nextgenbank.backend.repository;

import com.nextgenbank.backend.model.Account;
import com.nextgenbank.backend.model.Transaction;
import com.nextgenbank.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByFromAccountOrToAccountOrderByTimestampDesc(Account fromAccount, Account toAccount);
    List<Transaction> findAllByOrderByTimestampDesc();
    List<Transaction> findByInitiatorOrderByTimestampDesc(User initiator);
    List<Transaction> findByFromAccount_CustomerOrToAccount_CustomerOrderByTimestampDesc(User customer, User sameCustomer);
}