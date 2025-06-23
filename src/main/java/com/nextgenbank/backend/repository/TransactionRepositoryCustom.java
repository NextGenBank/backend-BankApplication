package com.nextgenbank.backend.repository;

import com.nextgenbank.backend.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface TransactionRepositoryCustom {
    Page<Transaction> findAllByUserIdWithFilters(
            Long userId,
            String iban,
            String name,
            String direction,
            String startDate,
            String endDate,
            BigDecimal amount,
            String amountFilter,
            Pageable pageable
    );
}