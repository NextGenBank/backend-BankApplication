package com.nextgenbank.backend.model.dto;

import com.nextgenbank.backend.model.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponseDto(
        Long transactionId,
        TransactionType transactionType,
        BigDecimal amount,
        LocalDateTime timestamp,
        String fromIban,
        String toIban
) { }
