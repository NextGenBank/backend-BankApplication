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
public interface UserRepository extends JpaRepository<User, Long>, UserRepositoryCustom {
    Optional<User> findByEmail(String email);
    Optional<User> findByBsnNumber(String bsnNumber);
    Optional<User> findByPhoneNumber(String phoneNumber);

    List<User> findByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndStatus(String firstName, String lastName, UserStatus userStatus);
    
    // Paginated queries for Employee features
    Page<User> findByRoleAndStatus(UserRole role, UserStatus status, Pageable pageable);
    List<User> findByRoleAndStatus(UserRole role, UserStatus status); // Keep non-paginated version for compatibility
    
    Page<User> findByRole(UserRole role, Pageable pageable);
    List<User> findByRole(UserRole role); // Keep non-paginated version for compatibility

    @Query("""
        SELECT DISTINCT u FROM User u
        LEFT JOIN FETCH u.accountsOwned
        WHERE u.status = :status AND u.role = :role
    """)
    List<User> findApprovedCustomersWithAccounts(@Param("status") UserStatus status, @Param("role") UserRole role);

    @Query("""
        SELECT u FROM User u
        LEFT JOIN FETCH u.accountsOwned
        WHERE LOWER(u.firstName) = LOWER(:firstName)
          AND LOWER(u.lastName) = LOWER(:lastName)
          AND u.status = :status
    """)
    List<User> findByNameAndStatusWithAccounts(@Param("firstName") String firstName,
                                               @Param("lastName") String lastName,
                                               @Param("status") UserStatus status);
}
