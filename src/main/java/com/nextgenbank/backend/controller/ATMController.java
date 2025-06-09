
package com.nextgenbank.backend.controller;

import com.nextgenbank.backend.model.Transaction;
import com.nextgenbank.backend.model.User;
import com.nextgenbank.backend.model.dto.TransactionDto;
import com.nextgenbank.backend.model.dto.TransactionResponseDto;
import com.nextgenbank.backend.security.CurrentUser;
import com.nextgenbank.backend.security.UserPrincipal;
import com.nextgenbank.backend.service.ATMService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/atm")
@CrossOrigin(origins = "http://localhost:5173")
public class ATMController {

    private final ATMService atmService;

    public ATMController(ATMService atmService) {
        this.atmService = atmService;
    }


    @PostMapping("/deposit")
    public ResponseEntity<?> createDeposit(
            @RequestBody TransactionDto dto,
            @CurrentUser UserPrincipal principal
    ) {
        if (dto.getToIban() == null || dto.getAmount() == null) {
            return ResponseEntity.badRequest().body("toIban and amount are required");
        }
        try {
            User user = principal.getUser();
            Transaction tx = atmService.doDeposit(
                    user,
                    dto.getToIban(),
                    dto.getAmount()
            );

            String toName    = tx.getToAccount().getCustomer().getFirstName() + " " +
                    tx.getToAccount().getCustomer().getLastName();
            String direction = tx.getTransactionType().name();

            return ResponseEntity.ok(new TransactionResponseDto(
                    tx.getTransactionId(),
                    tx.getTransactionType(),
                    tx.getAmount(),
                    tx.getTimestamp(),
                    null,
                    null,
                    tx.getToAccount().getIBAN(),
                    toName,
                    direction
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }


    @PostMapping("/withdraw")
    public ResponseEntity<?> createWithdraw(
            @RequestBody TransactionDto dto,
            @CurrentUser UserPrincipal principal
    ) {
        if (dto.getFromIban() == null || dto.getAmount() == null) {
            return ResponseEntity.badRequest().body("fromIban and amount are required");
        }
        try {
            User user = principal.getUser();
            Transaction tx = atmService.doWithdraw(
                    user,
                    dto.getFromIban(),
                    dto.getAmount(),
                    dto.getBills()
            );

            String fromName = tx.getFromAccount().getCustomer().getFirstName() + " " +
                    tx.getFromAccount().getCustomer().getLastName();
            String direction = tx.getTransactionType().name();

            return ResponseEntity.ok(new TransactionResponseDto(
                    tx.getTransactionId(),
                    tx.getTransactionType(),
                    tx.getAmount(),
                    tx.getTimestamp(),
                    tx.getFromAccount().getIBAN(),
                    fromName,
                    null,
                    null,
                    direction
            ));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }
}
