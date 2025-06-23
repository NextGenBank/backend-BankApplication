package com.nextgenbank.backend.repository;

import com.nextgenbank.backend.model.Transaction;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
public class TransactionRepositoryImpl implements TransactionRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<Transaction> findAllByUserIdWithFilters(
            Long userId,
            String iban,
            String name,
            String direction,
            String startDate,
            String endDate,
            BigDecimal amount,
            String amountFilter,
            Pageable pageable
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // Main query
        CriteriaQuery<Transaction> query = cb.createQuery(Transaction.class);
        Root<Transaction> root = query.from(Transaction.class);
        Join<Object, Object> fromAccount = root.join("fromAccount", JoinType.LEFT);
        Join<Object, Object> toAccount = root.join("toAccount", JoinType.LEFT);
        Join<Object, Object> fromCustomer = fromAccount.join("customer", JoinType.LEFT);
        Join<Object, Object> toCustomer = toAccount.join("customer", JoinType.LEFT);

        List<Predicate> predicates = new ArrayList<>();

        // Always include transactions related to the user
        Predicate userPredicate = cb.or(
                cb.equal(fromCustomer.get("userId"), userId),
                cb.equal(toCustomer.get("userId"), userId)
        );
        predicates.add(userPredicate);

        // Direction filter
        if (direction != null && !direction.isEmpty()) {
            switch (direction.toUpperCase()) {
                case "INCOMING" -> predicates.add(cb.and(
                        cb.equal(toCustomer.get("userId"), userId),
                        cb.or(cb.notEqual(fromCustomer.get("userId"), userId), cb.isNull(fromCustomer.get("userId")))
                ));
                case "OUTGOING" -> predicates.add(cb.and(
                        cb.equal(fromCustomer.get("userId"), userId),
                        cb.or(cb.notEqual(toCustomer.get("userId"), userId), cb.isNull(toCustomer.get("userId")))
                ));
                case "INTERNAL" -> predicates.add(cb.and(
                        cb.equal(fromCustomer.get("userId"), userId),
                        cb.equal(toCustomer.get("userId"), userId)
                ));
            }
        }

        // IBAN filter
        if (iban != null && !iban.isEmpty()) {
            String ibanPattern = "%" + iban.toLowerCase() + "%";
            predicates.add(cb.or(
                    cb.like(cb.lower(fromAccount.get("IBAN")), ibanPattern),
                    cb.like(cb.lower(toAccount.get("IBAN")), ibanPattern)
            ));
        }

        // Name filter
        if (name != null && !name.isEmpty()) {
            String namePattern = "%" + name.toLowerCase() + "%";
            predicates.add(cb.or(
                    cb.like(cb.lower(fromCustomer.get("firstName")), namePattern),
                    cb.like(cb.lower(fromCustomer.get("lastName")), namePattern),
                    cb.like(cb.lower(toCustomer.get("firstName")), namePattern),
                    cb.like(cb.lower(toCustomer.get("lastName")), namePattern)
            ));
        }

        // Date range filter
        if (startDate != null && !startDate.isEmpty()) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), LocalDate.parse(startDate).atStartOfDay()));
        }
        if (endDate != null && !endDate.isEmpty()) {
            predicates.add(cb.lessThan(root.get("timestamp"), LocalDate.parse(endDate).plusDays(1).atStartOfDay()));
        }

        // Amount filter
        if (amount != null && amountFilter != null) {
            switch (amountFilter.toUpperCase()) {
                case "EQUAL" -> predicates.add(cb.equal(root.get("amount"), amount));
                case "GREATER" -> predicates.add(cb.greaterThan(root.get("amount"), amount));
                case "LESS" -> predicates.add(cb.lessThan(root.get("amount"), amount));
            }
        }

        query.select(root).where(cb.and(predicates.toArray(new Predicate[0]))).distinct(true);

        List<Transaction> transactions = entityManager.createQuery(query)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        // Count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Transaction> countRoot = countQuery.from(Transaction.class);
        Join<Object, Object> countFrom = countRoot.join("fromAccount", JoinType.LEFT);
        Join<Object, Object> countTo = countRoot.join("toAccount", JoinType.LEFT);
        Join<Object, Object> countFromCustomer = countFrom.join("customer", JoinType.LEFT);
        Join<Object, Object> countToCustomer = countTo.join("customer", JoinType.LEFT);

        List<Predicate> countPredicates = new ArrayList<>();
        Predicate countUserPredicate = cb.or(
                cb.equal(countFromCustomer.get("userId"), userId),
                cb.equal(countToCustomer.get("userId"), userId)
        );
        countPredicates.add(countUserPredicate);

        if (direction != null && !direction.isEmpty()) {
            switch (direction.toUpperCase()) {
                case "INCOMING" -> countPredicates.add(cb.and(
                        cb.equal(countToCustomer.get("userId"), userId),
                        cb.or(cb.notEqual(countFromCustomer.get("userId"), userId), cb.isNull(countFromCustomer.get("userId")))
                ));
                case "OUTGOING" -> countPredicates.add(cb.and(
                        cb.equal(countFromCustomer.get("userId"), userId),
                        cb.or(cb.notEqual(countToCustomer.get("userId"), userId), cb.isNull(countToCustomer.get("userId")))
                ));
                case "INTERNAL" -> countPredicates.add(cb.and(
                        cb.equal(countFromCustomer.get("userId"), userId),
                        cb.equal(countToCustomer.get("userId"), userId)
                ));
            }
        }

        if (iban != null && !iban.isEmpty()) {
            String ibanPattern = "%" + iban.toLowerCase() + "%";
            countPredicates.add(cb.or(
                    cb.like(cb.lower(countFrom.get("IBAN")), ibanPattern),
                    cb.like(cb.lower(countTo.get("IBAN")), ibanPattern)
            ));
        }

        if (name != null && !name.isEmpty()) {
            String namePattern = "%" + name.toLowerCase() + "%";
            countPredicates.add(cb.or(
                    cb.like(cb.lower(countFromCustomer.get("firstName")), namePattern),
                    cb.like(cb.lower(countFromCustomer.get("lastName")), namePattern),
                    cb.like(cb.lower(countToCustomer.get("firstName")), namePattern),
                    cb.like(cb.lower(countToCustomer.get("lastName")), namePattern)
            ));
        }

        if (startDate != null && !startDate.isEmpty()) {
            countPredicates.add(cb.greaterThanOrEqualTo(countRoot.get("timestamp"), LocalDate.parse(startDate).atStartOfDay()));
        }
        if (endDate != null && !endDate.isEmpty()) {
            countPredicates.add(cb.lessThan(countRoot.get("timestamp"), LocalDate.parse(endDate).plusDays(1).atStartOfDay()));
        }

        if (amount != null && amountFilter != null) {
            switch (amountFilter.toUpperCase()) {
                case "EQUAL" -> countPredicates.add(cb.equal(countRoot.get("amount"), amount));
                case "GREATER" -> countPredicates.add(cb.greaterThan(countRoot.get("amount"), amount));
                case "LESS" -> countPredicates.add(cb.lessThan(countRoot.get("amount"), amount));
            }
        }

        countQuery.select(cb.countDistinct(countRoot)).where(cb.and(countPredicates.toArray(new Predicate[0])));
        Long total = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(transactions, pageable, total);
    }
}
