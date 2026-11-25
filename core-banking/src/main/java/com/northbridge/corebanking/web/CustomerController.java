package com.northbridge.corebanking.web;

import com.northbridge.corebanking.domain.Account;
import com.northbridge.corebanking.domain.BankingException;
import com.northbridge.corebanking.domain.Customer;
import com.northbridge.corebanking.service.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final AccountService accountService;
    private final CurrentUser currentUser;

    @Autowired
    public CustomerController(AccountService accountService, CurrentUser currentUser) {
        this.accountService = accountService;
        this.currentUser = currentUser;
    }

    @RequestMapping(method = RequestMethod.GET)
    @PreAuthorize("hasAnyRole('TELLER','OPS')")
    public List<Customer> search(@RequestParam("lastName") String lastName) {
        return accountService.findCustomersByLastName(lastName);
    }

    @RequestMapping(value = "/{cif}", method = RequestMethod.GET)
    public Customer get(@PathVariable String cif) {
        requireOwnCif(cif);
        return accountService.getCustomer(cif);
    }

    @RequestMapping(value = "/{cif}/accounts", method = RequestMethod.GET)
    public List<Account> accounts(@PathVariable String cif) {
        requireOwnCif(cif);
        return accountService.getAccountsForCustomer(cif);
    }

    private void requireOwnCif(String cif) {
        if (currentUser.isCustomer() && !cif.equals(currentUser.cif())) {
            throw new BankingException("FORBIDDEN", 403, "Not your customer record");
        }
    }
}
