package com.nextgenbank.backend.repository;

import com.nextgenbank.backend.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, String> {

}