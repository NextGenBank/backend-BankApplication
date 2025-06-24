package com.nextgenbank.backend.repository;

import com.nextgenbank.backend.model.Account;
import com.nextgenbank.backend.model.Transaction;
import com.nextgenbank.backend.model.TransactionType;
import com.nextgenbank.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {
    List<Transaction> findByFromAccountOrToAccountOrderByTimestampDesc(Account fromAccount, Account toAccount);
    List<Transaction> findAllByOrderByTimestampDesc();
    List<Transaction> findByInitiatorOrderByTimestampDesc(User initiator);
    List<Transaction> findByFromAccount_CustomerOrToAccount_CustomerOrderByTimestampDesc(User customer, User sameCustomer);

    // Paginated methods
    Page<Transaction> findAllByOrderByTimestampDesc(Pageable pageable);
    Page<Transaction> findByInitiatorOrderByTimestampDesc(User initiator, Pageable pageable);
    Page<Transaction> findByFromAccount_CustomerOrToAccount_CustomerOrderByTimestampDesc(User customer, User sameCustomer, Pageable pageable);

    @Query("""
    SELECT t FROM Transaction t
    LEFT JOIN t.fromAccount fa
    LEFT JOIN t.toAccount ta
    LEFT JOIN fa.customer fromCustomer
    LEFT JOIN ta.customer toCustomer
    WHERE (fromCustomer.userId = :userId OR toCustomer.userId = :userId)
      AND (:iban IS NULL OR LOWER(fa.IBAN) LIKE LOWER(CONCAT('%', :iban, '%')) OR LOWER(ta.IBAN) LIKE LOWER(CONCAT('%', :iban, '%')))
      AND (:name IS NULL OR 
           (LOWER(fromCustomer.firstName) LIKE LOWER(CONCAT('%', :name, '%')) 
            OR LOWER(fromCustomer.lastName) LIKE LOWER(CONCAT('%', :name, '%'))
            OR LOWER(toCustomer.firstName) LIKE LOWER(CONCAT('%', :name, '%'))
            OR LOWER(toCustomer.lastName) LIKE LOWER(CONCAT('%', :name, '%'))))
      AND (:direction IS NULL OR 
           (:direction = 'INCOMING' AND toCustomer.userId = :userId AND (fromCustomer.userId IS NULL OR fromCustomer.userId != :userId))
           OR (:direction = 'OUTGOING' AND fromCustomer.userId = :userId AND (toCustomer.userId IS NULL OR toCustomer.userId != :userId))
           OR (:direction = 'INTERNAL' AND fromCustomer.userId = :userId AND toCustomer.userId = :userId))
      AND (:startDate IS NULL OR CAST(t.timestamp AS date) >= :startDate)
      AND (:endDate IS NULL OR CAST(t.timestamp AS date) <= :endDate)
      AND (:amount IS NULL OR 
           (:amountOperation IS NULL OR :amountOperation = 'eq' AND t.amount = :amount)
           OR (:amountOperation = 'lt' AND t.amount < :amount)
           OR (:amountOperation = 'gt' AND t.amount > :amount))
    ORDER BY t.timestamp DESC
""")
    Page<Transaction> findAllByUserIdWithFilters(
            @Param("userId") Long userId,
            @Param("iban") String iban,
            @Param("name") String name,
            @Param("direction") String direction,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("amount") BigDecimal amount,
            @Param("amountOperation") String amountOperation,
            Pageable pageable
    );


    @Query("""
       SELECT t
       FROM Transaction t
       WHERE t.amount = 0
    """)
    Page<Transaction> findPendingTransactions(Pageable pageable);
}
