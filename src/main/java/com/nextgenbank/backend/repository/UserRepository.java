package com.nextgenbank.backend.repository;

import com.nextgenbank.backend.model.User;
import com.nextgenbank.backend.model.UserRole;
import com.nextgenbank.backend.model.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
    Optional<User> findByBsnNumber(String bsnNumber);
    Optional<User> findByPhoneNumber(String phoneNumber);

    // Paginated queries for Employee features
    Page<User> findByRoleAndStatus(UserRole role, UserStatus status, Pageable pageable);
    List<User> findByRoleAndStatus(UserRole role, UserStatus status); // Keep non-paginated version for compatibility
    
    Page<User> findByRole(UserRole role, Pageable pageable);
    List<User> findByRole(UserRole role); // Keep non-paginated version for compatibility

    @Query("""
        SELECT DISTINCT u FROM User u
        LEFT JOIN FETCH u.accountsOwned a
        WHERE u.status = 'APPROVED'
        AND (:name IS NULL OR 
             LOWER(u.firstName) LIKE LOWER(CONCAT('%', :name, '%')) OR 
             LOWER(u.lastName) LIKE LOWER(CONCAT('%', :name, '%')))
        AND (:iban IS NULL OR LOWER(a.IBAN) LIKE LOWER(CONCAT('%', :iban, '%')))
    """)
    Page<User> findApprovedUsersWithAccounts(
            @Param("name") String name,
            @Param("iban") String iban,
            Pageable pageable);
}