package com.nextgenbank.backend.steps;

import com.nextgenbank.backend.model.Account;
import com.nextgenbank.backend.model.User;
import com.nextgenbank.backend.repository.AccountRepository;
import com.nextgenbank.backend.service.ATMService;
import io.cucumber.java.en.*;
import org.junit.jupiter.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

@SpringBootTest
public class ATMStepDefinition {

    @Autowired
    private ATMService atmService;

    @Autowired
    private AccountRepository accountRepository;

    private User testUser;
    private Exception caughtException;
    private BigDecimal resultAmount;

    @Given("a user with account IBAN {string} and balance {int}")
    public void a_user_with_account_and_balance(String iban, Integer balance) {
        testUser = new User();
        testUser.setUserId(1L);

        Account acc = new Account();
        acc.setIBAN(iban);
        acc.setBalance(BigDecimal.valueOf(balance));
        acc.setCustomer(testUser);

        accountRepository.save(acc);
    }

    @When("the user deposits {int} to {string}")
    public void the_user_deposits(Integer amount, String toIban) {
        try {
            var tx = atmService.doDeposit(testUser, toIban, BigDecimal.valueOf(amount));
            resultAmount = tx.getAmount();
        } catch (Exception ex) {
            caughtException = ex;
        }
    }

    @When("the user tries to withdraw {int} from {string}")
    public void the_user_withdraws(Integer amount, String fromIban) {
        try {
            var tx = atmService.doWithdraw(testUser, fromIban, BigDecimal.valueOf(amount), null);
            resultAmount = tx.getAmount();
        } catch (Exception ex) {
            caughtException = ex;
        }
    }

    @Then("the transaction is successful with amount {int}")
    public void transaction_successful(Integer expected) {
        Assertions.assertEquals(BigDecimal.valueOf(expected), resultAmount);
    }

    @Then("the transaction fails with message {string}")
    public void transaction_fails(String msg) {
        Assertions.assertNotNull(caughtException);
        Assertions.assertTrue(caughtException.getMessage().contains(msg));
    }
}
