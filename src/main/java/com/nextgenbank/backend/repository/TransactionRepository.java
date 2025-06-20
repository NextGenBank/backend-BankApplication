package com.nextgenbank.backend.repository;

import com.nextgenbank.backend.model.Account;
import com.nextgenbank.backend.model.Transaction;
import com.nextgenbank.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction>, TransactionRepositoryCustom {
    // Non-paginated methods (for backward compatibility)
    List<Transaction> findByFromAccountOrToAccountOrderByTimestampDesc(Account fromAccount, Account toAccount);
    List<Transaction> findAllByOrderByTimestampDesc();
    List<Transaction> findByInitiatorOrderByTimestampDesc(User initiator);
    List<Transaction> findByFromAccount_CustomerOrToAccount_CustomerOrderByTimestampDesc(User customer, User sameCustomer);
    
    // Paginated methods
    Page<Transaction> findAllByOrderByTimestampDesc(Pageable pageable);
    Page<Transaction> findByInitiatorOrderByTimestampDesc(User initiator, Pageable pageable);
    Page<Transaction> findByFromAccount_CustomerOrToAccount_CustomerOrderByTimestampDesc(User customer, User sameCustomer, Pageable pageable);

    @Query("""
       SELECT t
       FROM Transaction t
       LEFT JOIN t.fromAccount fa
       LEFT JOIN t.toAccount   ta
       WHERE (fa.customer.userId = :userId) 
          OR (ta.customer.userId = :userId)
    """)
    Page<Transaction> findAllByUserIdPaged(@Param("userId") Long userId, Pageable pageable);
    
    @Query("""
       SELECT t
       FROM Transaction t
       WHERE t.amount = 0
    """)
    Page<Transaction> findPendingTransactions(Pageable pageable);
}
