package com.nextgenbank.backend.repository;

import com.nextgenbank.backend.model.User;
import java.util.List;

public interface UserRepositoryCustom {
    List<User> findApprovedUsersWithAccounts(String firstName, String lastName, String iban);
    List<User> findAllApprovedCustomersWithAccounts();
}