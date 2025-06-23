package com.nextgenbank.backend.repository;

import com.nextgenbank.backend.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserRepositoryCustom {
    Page<User> findApprovedUsersWithAccounts(String name, String iban, Pageable pageable);
}