package com.bofa.corebanking.web;

import com.bofa.common.util.BankDates;
import com.bofa.corebanking.domain.Account;
import com.bofa.corebanking.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;
    private final CurrentUser currentUser;

    @Autowired
    public AccountController(AccountService accountService, CurrentUser currentUser) {
        this.accountService = accountService;
        this.currentUser = currentUser;
    }

    @RequestMapping(value = "/{accountNumber}", method = RequestMethod.GET)
    public Account get(@PathVariable String accountNumber) {
        return accountService.getAccount(accountNumber);
    }

    @RequestMapping(value = "/{accountNumber}/transactions", method = RequestMethod.GET)
    public Map<String, Object> transactions(@PathVariable String accountNumber,
                                            @RequestParam(value = "from", required = false) String from,
                                            @RequestParam(value = "to", required = false) String to,
                                            @RequestParam(value = "page", defaultValue = "0") int page,
                                            @RequestParam(value = "size", defaultValue = "50") int size) {
        return accountService.getTransactions(accountNumber,
                from == null ? null : BankDates.parseDate(from),
                to == null ? null : BankDates.parseDate(to),
                page, size);
    }

    @RequestMapping(value = "/{accountNumber}/holds", method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> placeHold(@PathVariable String accountNumber, @RequestBody HoldRequest request) {
        long id = accountService.placeHold(accountNumber, new BigDecimal(request.amount), request.reason,
                BankDates.parseDate(request.releaseOn), currentUser.username());
        Map<String, Object> body = new HashMap<String, Object>();
        body.put("holdId", id);
        body.put("accountNumber", accountNumber);
        body.put("amount", request.amount);
        body.put("reason", request.reason);
        body.put("releaseOn", request.releaseOn);
        body.put("status", "ACTIVE");
        return body;
    }

    public static class HoldRequest {
        public String amount;
        public String reason;
        public String releaseOn;
    }
}
