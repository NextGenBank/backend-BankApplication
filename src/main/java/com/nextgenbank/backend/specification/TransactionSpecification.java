package com.nextgenbank.backend.specification;

import com.nextgenbank.backend.model.Transaction;
import com.nextgenbank.backend.model.TransactionDirection;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;

public class TransactionSpecification {

    public static Specification<Transaction> filterByCriteria(
            Long userId,
            String iban,
            String name,
            TransactionDirection direction,
            LocalDate startDate,
            LocalDate endDate,
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

            if (direction != null) {
                Path<Long> fromUserId = fromCustomer.get("userId");
                Path<Long> toUserId = toCustomer.get("userId");

                switch (direction) {
                    case INCOMING -> predicate = cb.and(predicate,
                            cb.and(
                                    cb.equal(toUserId, userId),
                                    cb.or(
                                            cb.notEqual(fromUserId, userId),
                                            cb.isNull(fromUserId)
                                    )
                            )
                    );
                    case OUTGOING -> predicate = cb.and(predicate,
                            cb.and(
                                    cb.equal(fromUserId, userId),
                                    cb.notEqual(toUserId, userId)
                            )
                    );
                    case INTERNAL -> predicate = cb.and(predicate,
                            cb.and(
                                    cb.equal(fromUserId, userId),
                                    cb.equal(toUserId, userId)
                            )
                    );
                }
            }

            if (startDate != null) {
                predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("timestamp"), startDate.atStartOfDay()));
            }

            if (endDate != null) {
                predicate = cb.and(predicate, cb.lessThan(root.get("timestamp"), endDate.plusDays(1).atStartOfDay()));
            }

            if (amount != null && amountFilter != null) {
                switch (amountFilter.toUpperCase()) {
                    case "EQUAL" -> predicate = cb.and(predicate, cb.equal(root.get("amount"), amount));
                    case "GREATER" -> predicate = cb.and(predicate, cb.greaterThan(root.get("amount"), amount));
                    case "LESS" -> predicate = cb.and(predicate, cb.lessThan(root.get("amount"), amount));
                }
            }

            return predicate;
        };
    }
}