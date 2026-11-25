package com.bofa.corebanking.web;

import com.bofa.corebanking.domain.Transaction;
import com.bofa.corebanking.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final AccountService accountService;

    @Autowired
    public TransactionController(AccountService accountService) {
        this.accountService = accountService;
    }

    @RequestMapping(value = "/search", method = RequestMethod.GET)
    public List<Transaction> search(@RequestParam("q") String q,
                                    @RequestParam(value = "accountNumber", required = false) String accountNumber) {
        return accountService.searchTransactions(q, accountNumber);
    }
}
