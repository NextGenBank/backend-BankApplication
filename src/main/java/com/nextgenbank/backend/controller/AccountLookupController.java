package com.nextgenbank.backend.controller;

import com.nextgenbank.backend.model.User;
import com.nextgenbank.backend.model.UserStatus;
import com.nextgenbank.backend.model.Account;
import com.nextgenbank.backend.model.dto.AccountLookupDto;
import com.nextgenbank.backend.repository.UserRepository;
import com.nextgenbank.backend.repository.AccountRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountLookupController {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;

    public AccountLookupController(UserRepository userRepository, AccountRepository accountRepository) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
    }

    @GetMapping("/lookup")
    public ResponseEntity<?> lookupIBAN(
            @RequestParam String firstName,
            @RequestParam String lastName,
            Authentication authentication) {

        // Get the logged-in user (optional use)
        String requesterEmail = authentication.getName();

        // Find the target user
        List<User> matchedUsers = userRepository.findByFirstNameIgnoreCaseAndLastNameIgnoreCaseAndStatus(
                firstName, lastName, UserStatus.APPROVED);

        if (matchedUsers.isEmpty()) {
            return ResponseEntity.status(404).body("No approved user found with that name.");
        }

        // Get the IBANs
        User matchedUser = matchedUsers.get(0); // assume first match only
        List<Account> accounts = accountRepository.findByCustomer(matchedUser);

        List<String> ibans = accounts.stream()
                .map(Account::getIBAN)
                .toList();

        AccountLookupDto result = new AccountLookupDto(
                matchedUser.getFirstName(),
                matchedUser.getLastName(),
                ibans
        );

        return ResponseEntity.ok(result);
    }
}
