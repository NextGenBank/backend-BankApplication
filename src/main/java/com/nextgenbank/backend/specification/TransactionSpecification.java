package com.nextgenbank.backend.specification;

import com.nextgenbank.backend.model.Transaction;
import com.nextgenbank.backend.model.TransactionType;

import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransactionSpecification {

    public static Specification<Transaction> filterByCriteria(
            String iban,
            String name,
            TransactionType type,
            LocalDateTime startDate,
            LocalDateTime endDate,
            BigDecimal amount,
            String amountFilter
    ) {
        return (Root<Transaction> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            Join<Object, Object> fromAccount = root.join("fromAccount", JoinType.LEFT);
            Join<Object, Object> toAccount = root.join("toAccount", JoinType.LEFT);
            Join<Object, Object> fromCustomer = fromAccount.join("customer", JoinType.LEFT);
            Join<Object, Object> toCustomer = toAccount.join("customer", JoinType.LEFT);

            Predicate predicate = cb.conjunction();

            if (iban != null && !iban.isEmpty()) {
                String ibanPattern = iban.toLowerCase() + "%";
                predicate = cb.and(predicate, cb.or(
                        cb.like(cb.lower(fromAccount.get("IBAN")), ibanPattern),
                        cb.like(cb.lower(toAccount.get("IBAN")), ibanPattern)
                ));
            }

            if (name != null && !name.isEmpty()) {
                String lowerName = "%" + name.toLowerCase() + "%";
                predicate = cb.and(predicate, cb.or(
                        cb.like(cb.lower(fromCustomer.get("firstName")), lowerName),
                        cb.like(cb.lower(fromCustomer.get("lastName")), lowerName),
                        cb.like(cb.lower(toCustomer.get("firstName")), lowerName),
                        cb.like(cb.lower(toCustomer.get("lastName")), lowerName)
                ));
            }

            if (type != null) {
                predicate = cb.and(predicate, cb.equal(root.get("transactionType"), type));
            }

            if (startDate != null) {
                predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("timestamp"), startDate));
            }

            if (endDate != null) {
                predicate = cb.and(predicate, cb.lessThanOrEqualTo(root.get("timestamp"), endDate));
            }

            if (amount != null && amountFilter != null) {
                switch (amountFilter) {
                    case "EQUAL" -> predicate = cb.and(predicate, cb.equal(root.get("amount"), amount));
                    case "GREATER" -> predicate = cb.and(predicate, cb.greaterThan(root.get("amount"), amount));
                    case "LESS" -> predicate = cb.and(predicate, cb.lessThan(root.get("amount"), amount));
                }
            }

            return predicate;
        };
    }
}
