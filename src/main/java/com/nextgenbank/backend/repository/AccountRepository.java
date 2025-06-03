package com.nextgenbank.backend.repository;

import com.nextgenbank.backend.model.Account;
import com.nextgenbank.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountRepository extends JpaRepository<Account, String> {

    List<Account> findByCustomer(User user);

    List<Account> findByCustomerIn(List<User> users);
}