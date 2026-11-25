package com.northbridge.corebanking.web;

import com.northbridge.corebanking.domain.BankingException;
import com.northbridge.corebanking.domain.Transfer;
import com.northbridge.corebanking.service.AccountService;
import com.northbridge.corebanking.service.TransferService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/transfers")
public class TransferController {

    private final TransferService transferService;
    private final AccountService accountService;
    private final CurrentUser currentUser;

    @Autowired
    public TransferController(TransferService transferService, AccountService accountService, CurrentUser currentUser) {
        this.transferService = transferService;
        this.accountService = accountService;
        this.currentUser = currentUser;
    }

    @RequestMapping(method = RequestMethod.POST)
    @ResponseStatus(HttpStatus.CREATED)
    public Transfer create(@RequestBody TransferRequest request) {
        if (currentUser.isCustomer()) {
            String owner = accountService.getAccount(request.fromAccount).getCif();
            if (!owner.equals(currentUser.cif())) {
                throw new BankingException("FORBIDDEN", 403, "Cannot transfer from an account you do not own");
            }
        }
        return transferService.transfer(request.fromAccount, request.toAccount, new BigDecimal(request.amount),
                request.memo, request.idempotencyKey, currentUser.username(), currentUser.isCustomer());
    }

    @RequestMapping(value = "/{transferId}", method = RequestMethod.GET)
    public Transfer get(@PathVariable String transferId) {
        return transferService.get(transferId);
    }

    public static class TransferRequest {
        public String fromAccount;
        public String toAccount;
        public String amount;
        public String memo;
        public String idempotencyKey;
    }
}
