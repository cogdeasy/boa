package com.northbridge.corebanking.service;

import com.northbridge.common.util.BankDates;
import com.northbridge.corebanking.dao.AccountDao;
import com.northbridge.corebanking.dao.CustomerDao;
import com.northbridge.corebanking.dao.HoldDao;
import com.northbridge.corebanking.dao.TransactionDao;
import com.northbridge.corebanking.domain.Account;
import com.northbridge.corebanking.domain.BankingException;
import com.northbridge.corebanking.domain.Customer;
import com.northbridge.corebanking.domain.Transaction;
import com.northbridge.common.util.AuditLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AccountService {

    private final AccountDao accountDao;
    private final CustomerDao customerDao;
    private final TransactionDao transactionDao;
    private final HoldDao holdDao;

    @Autowired
    public AccountService(AccountDao accountDao, CustomerDao customerDao, TransactionDao transactionDao, HoldDao holdDao) {
        this.accountDao = accountDao;
        this.customerDao = customerDao;
        this.transactionDao = transactionDao;
        this.holdDao = holdDao;
    }

    public Customer getCustomer(String cif) {
        Customer c = customerDao.findByCif(cif);
        if (c == null) {
            throw new BankingException("CUSTOMER_NOT_FOUND", 404, "Customer " + cif + " not found");
        }
        return c;
    }

    public List<Customer> findCustomersByLastName(String lastName) {
        return customerDao.searchByLastName(lastName);
    }

    public List<Account> getAccountsForCustomer(String cif) {
        getCustomer(cif);
        return accountDao.findByCif(cif);
    }

    public Account getAccount(String accountNumber) {
        Account a = accountDao.findByNumber(accountNumber);
        if (a == null) {
            throw BankingException.notFound("Account " + accountNumber);
        }
        return a;
    }

    public Map<String, Object> getTransactions(String accountNumber, Date from, Date to, int page, int size) {
        getAccount(accountNumber);
        if (to == null) {
            to = BankDates.businessDate(new Date());
        }
        if (from == null) {
            from = BankDates.addDays(to, -90);
        }
        List<Transaction> content = transactionDao.findByAccount(accountNumber, from, to, page, size);
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("content", content);
        result.put("page", page);
        result.put("size", size);
        result.put("total", transactionDao.countByAccount(accountNumber, from, to));
        return result;
    }

    public List<Transaction> searchTransactions(String query, String accountNumber) {
        return transactionDao.search(query, accountNumber);
    }

    public long placeHold(String accountNumber, BigDecimal amount, String reason, Date releaseOn, String teller) {
        Account account = getAccount(accountNumber);
        if (!account.isOpen()) {
            throw new BankingException("ACCOUNT_NOT_OPEN", 422, "Account is " + account.getStatus());
        }
        if (amount.signum() <= 0) {
            throw new BankingException("INVALID_AMOUNT", 400, "Hold amount must be positive");
        }
        long id = holdDao.insert(accountNumber, amount, reason, teller, releaseOn);
        AuditLogger.record("AUD-HOLD-PLACED", teller, "account=" + accountNumber + " amount=" + amount + " reason=" + reason);
        return id;
    }
}
