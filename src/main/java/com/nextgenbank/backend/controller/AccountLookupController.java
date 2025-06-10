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
import com.nextgenbank.backend.model.UserRole;


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
            @RequestParam String firstName, //GET /api/accounts/lookup?firstName=Alice&lastName=Smith
            @RequestParam String lastName,
            Authentication authentication) {

        // Get the logged-in user (opt)
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

        List<String> ibans = accounts.stream() //sooo this changes List<Account> → List<String>  (of IBANs)
                .map(Account::getIBAN) //shortcut for .map(account -> account.getIBAN())
                .toList(); //then this collects all the transformed results into a new list  List<String> ibans = [ "NL91ABNA0417164300", "NL54INGB0001234567" ]


        AccountLookupDto result = new AccountLookupDto(
                matchedUser.getFirstName(),
                matchedUser.getLastName(),
                ibans
        );

        return ResponseEntity.ok(result);
    }

    @GetMapping("/all-iban-users")
    public ResponseEntity<?> getAllUsersWithIbans(Authentication authentication) {
        // get all approved customers
        List<User> approvedCustomers = userRepository.findByRoleAndStatus(
                UserRole.CUSTOMER, UserStatus.APPROVED);

        // transform each customer to an AccountLookupDto with their IBANs
        List<AccountLookupDto> results = approvedCustomers.stream()
                .map(user -> {
                    // get all accounts for this user and extract IBANs
                    List<String> ibans = accountRepository.findByCustomer(user)
                            .stream()
                            .map(Account::getIBAN)
                            .toList();

                    // create DTO for this user
                    return new AccountLookupDto(
                            user.getFirstName(),
                            user.getLastName(),
                            ibans
                    );
                })
                .toList();

        return ResponseEntity.ok(results);
    }

}
