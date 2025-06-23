//package com.nextgenbank.backend.specification;
//
//import com.nextgenbank.backend.model.Transaction;
//import com.nextgenbank.backend.model.TransactionDirection;
//import jakarta.persistence.criteria.*;
//import org.springframework.data.jpa.domain.Specification;
//
//import java.math.BigDecimal;
//import java.time.LocalDate;
//
//public class TransactionSpecification {
//
//    public static Specification<Transaction> filterByCriteria(
//            Long userId,
//            String iban,
//            String name,
//            TransactionDirection direction,
//            LocalDate startDate,
//            LocalDate endDate,
//            BigDecimal amount,
//            String amountFilter
//    ) {
//        return (Root<Transaction> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
//            Join<Object, Object> fromAccount = root.join("fromAccount", JoinType.LEFT);
//            Join<Object, Object> toAccount = root.join("toAccount", JoinType.LEFT);
//            Join<Object, Object> fromCustomer = fromAccount.join("customer", JoinType.LEFT);
//            Join<Object, Object> toCustomer = toAccount.join("customer", JoinType.LEFT);
//
//            Predicate predicate = cb.conjunction();
//
//            predicate = addIbanFilter(cb, predicate, fromAccount, toAccount, iban);
//            predicate = addNameFilter(cb, predicate, fromCustomer, toCustomer, name);
//            predicate = addDirectionFilter(cb, predicate, fromCustomer, toCustomer, userId, direction);
//            predicate = addDateRangeFilter(cb, predicate, root, startDate, endDate);
//            predicate = addAmountFilter(cb, predicate, root, amount, amountFilter);
//
//            return predicate;
//        };
//    }
//
//    private static Predicate addIbanFilter(CriteriaBuilder cb, Predicate predicate,
//                                           Path<?> fromAccount, Path<?> toAccount, String iban) {
//        if (iban != null && !iban.isEmpty()) {
//            String pattern = iban.toLowerCase() + "%";
//            return cb.and(predicate, cb.or(
//                    cb.like(cb.lower(fromAccount.get("IBAN")), pattern),
//                    cb.like(cb.lower(toAccount.get("IBAN")), pattern)
//            ));
//        }
//        return predicate;
//    }
//
//    private static Predicate addNameFilter(CriteriaBuilder cb, Predicate predicate,
//                                           Path<?> fromCustomer, Path<?> toCustomer, String name) {
//        if (name != null && !name.isEmpty()) {
//            String pattern = "%" + name.toLowerCase() + "%";
//            return cb.and(predicate, cb.or(
//                    cb.like(cb.lower(fromCustomer.get("firstName")), pattern),
//                    cb.like(cb.lower(fromCustomer.get("lastName")), pattern),
//                    cb.like(cb.lower(toCustomer.get("firstName")), pattern),
//                    cb.like(cb.lower(toCustomer.get("lastName")), pattern)
//            ));
//        }
//        return predicate;
//    }
//
//    private static Predicate addDirectionFilter(CriteriaBuilder cb, Predicate predicate,
//                                                Path<?> fromCustomer, Path<?> toCustomer, Long userId, TransactionDirection direction) {
//        if (direction == null) return predicate;
//
//        Path<Long> fromUserId = fromCustomer.get("userId");
//        Path<Long> toUserId = toCustomer.get("userId");
//
//        return switch (direction) {
//            case INCOMING -> cb.and(predicate,
//                    cb.and(cb.equal(toUserId, userId),
//                            cb.or(cb.notEqual(fromUserId, userId), cb.isNull(fromUserId)))
//            );
//            case OUTGOING -> cb.and(predicate,
//                    cb.and(cb.equal(fromUserId, userId),
//                            cb.notEqual(toUserId, userId))
//            );
//            case INTERNAL -> cb.and(predicate,
//                    cb.and(cb.equal(fromUserId, userId),
//                            cb.equal(toUserId, userId))
//            );
//        };
//    }
//
//    private static Predicate addDateRangeFilter(CriteriaBuilder cb, Predicate predicate,
//                                                Root<Transaction> root, LocalDate start, LocalDate end) {
//        LocalDate today = LocalDate.now();
//        LocalDate limitStartDate = today.minusMonths(18);
//
//        if (start != null) {
//            if (start.isBefore(limitStartDate)) {
//                start = limitStartDate; // limit how far back they can go
//            }
//            predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("timestamp"), start.atStartOfDay()));
//        }
//
//        if (end != null) {
//            if (end.isAfter(today)) {
//                end = today; // prevent filtering for future transactions
//            }
//            predicate = cb.and(predicate, cb.lessThan(root.get("timestamp"), end.plusDays(1).atStartOfDay()));
//        }
//
//        return predicate;
//    }
//
//    private static Predicate addAmountFilter(CriteriaBuilder cb, Predicate predicate,
//                                             Root<Transaction> root, BigDecimal amount, String filter) {
//        if (amount != null && filter != null) {
//            return switch (filter.toUpperCase()) {
//                case "EQUAL" -> cb.and(predicate, cb.equal(root.get("amount"), amount));
//                case "GREATER" -> cb.and(predicate, cb.greaterThan(root.get("amount"), amount));
//                case "LESS" -> cb.and(predicate, cb.lessThan(root.get("amount"), amount));
//                default -> predicate;
//            };
//        }
//        return predicate;
//    }
//}
