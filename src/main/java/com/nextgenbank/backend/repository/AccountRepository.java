package com.nextgenbank.backend.repository;

import com.nextgenbank.backend.model.Account;
import com.nextgenbank.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountRepository extends JpaRepository<Account, String> {
    // Найти все счета, которые принадлежат данному клиенту
    List<Account> findByCustomer(User customer);
}