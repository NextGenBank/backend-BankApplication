package com.nextgenbank.backend.repository;

import com.nextgenbank.backend.model.Account;
import com.nextgenbank.backend.model.User;
import com.nextgenbank.backend.model.UserStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public class UserRepositoryImpl implements UserRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public List<User> findApprovedUsersWithAccounts(String firstName, String lastName, String iban) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<User> query = cb.createQuery(User.class);
        Root<User> user = query.from(User.class);
        Join<User, Account> accounts = user.join("accountsOwned", JoinType.LEFT);

        Predicate predicate = cb.equal(user.get("status"), UserStatus.APPROVED);

        // Name filter (search first OR last name using LIKE)
        if (firstName != null && !firstName.isBlank()) {
            String nameFilter = "%" + firstName.toLowerCase() + "%";
            Predicate firstMatch = cb.like(cb.lower(user.get("firstName")), nameFilter);
            Predicate lastMatch = cb.like(cb.lower(user.get("lastName")), nameFilter);
            predicate = cb.and(predicate, cb.or(firstMatch, lastMatch));
        }

        // IBAN filter
        if (iban != null && !iban.isBlank()) {
            predicate = cb.and(predicate,
                    cb.like(cb.lower(accounts.get("IBAN")), "%" + iban.toLowerCase() + "%"));
        }

        query.select(user).distinct(true).where(predicate);
        return entityManager.createQuery(query).getResultList();
    }


    @Override
    public List<User> findAllApprovedCustomersWithAccounts() {
        String jpql = """
            SELECT DISTINCT u FROM User u 
            LEFT JOIN FETCH u.accountsOwned a 
            WHERE u.role = 'CUSTOMER' 
            AND u.status = 'APPROVED'""";

        return entityManager.createQuery(jpql, User.class).getResultList();
    }
}