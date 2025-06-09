package com.nextgenbank.backend.repository;

import com.nextgenbank.backend.model.User;
import com.nextgenbank.backend.model.UserRole;
import com.nextgenbank.backend.model.UserStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByBsnNumber(String bsnNumber);
    Optional<User> findByPhoneNumber(String phoneNumber);

    List<User> findByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndStatus(String firstName, String lastName, UserStatus userStatus);
    List<User> findByRoleAndStatus(UserRole role, UserStatus status);
    List<User> findByRole(UserRole role);
}