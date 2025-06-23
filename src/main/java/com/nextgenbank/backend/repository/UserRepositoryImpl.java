package com.nextgenbank.backend.repository;

import com.nextgenbank.backend.model.Account;
import com.nextgenbank.backend.model.User;
import com.nextgenbank.backend.model.UserStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class UserRepositoryImpl implements UserRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<User> findApprovedUsersWithAccounts(String name, String iban, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<User> cq = cb.createQuery(User.class);
        Root<User> user = cq.from(User.class);
        user.fetch("accountsOwned", JoinType.LEFT);

        Predicate predicate = cb.equal(user.get("status"), UserStatus.APPROVED);

        if (name != null && !name.isBlank()) {
            String pattern = "%" + name.toLowerCase() + "%";
            Predicate firstNameMatch = cb.like(cb.lower(user.get("firstName")), pattern);
            Predicate lastNameMatch = cb.like(cb.lower(user.get("lastName")), pattern);
            predicate = cb.and(predicate, cb.or(firstNameMatch, lastNameMatch));
        }

        if (iban != null && !iban.isBlank()) {
            Join<User, Account> accounts = user.join("accountsOwned", JoinType.LEFT);
            predicate = cb.and(predicate,
                    cb.like(cb.lower(accounts.get("IBAN")), "%" + iban.toLowerCase() + "%"));
        }

        cq.select(user).distinct(true).where(predicate);

        // count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<User> countRoot = countQuery.from(User.class);
        Predicate countPredicate = cb.equal(countRoot.get("status"), UserStatus.APPROVED);

        if (name != null && !name.isBlank()) {
            String pattern = "%" + name.toLowerCase() + "%";
            Predicate firstNameMatch = cb.like(cb.lower(countRoot.get("firstName")), pattern);
            Predicate lastNameMatch = cb.like(cb.lower(countRoot.get("lastName")), pattern);
            countPredicate = cb.and(countPredicate, cb.or(firstNameMatch, lastNameMatch));
        }

        if (iban != null && !iban.isBlank()) {
            Join<User, Account> countAccounts = countRoot.join("accountsOwned", JoinType.LEFT);
            countPredicate = cb.and(countPredicate,
                    cb.like(cb.lower(countAccounts.get("IBAN")), "%" + iban.toLowerCase() + "%"));
        }

        countQuery.select(cb.countDistinct(countRoot)).where(countPredicate);

        List<User> resultList = entityManager.createQuery(cq)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        Long total = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(resultList, pageable, total);
    }
}
