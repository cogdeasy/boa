package com.bofa.corebanking.service;

import com.bofa.corebanking.dao.AccountDao;
import com.bofa.corebanking.dao.ProductLimitDao;
import com.bofa.corebanking.dao.TransactionDao;
import com.bofa.corebanking.dao.TransferDao;
import com.bofa.corebanking.domain.Account;
import com.bofa.corebanking.domain.BankingException;
import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class TransferServiceTest {

    @Test
    public void destinationProductWithoutLimitFailsClosed() {
        AccountDao accounts = mock(AccountDao.class);
        TransferDao transfers = mock(TransferDao.class);
        ProductLimitDao limits = mock(ProductLimitDao.class);
        when(accounts.findByNumber("104000123455")).thenReturn(account("104000123455", "DDA-CHK"));
        when(accounts.findByNumber("440000999990")).thenReturn(account("440000999990", "CC-AMEX"));
        when(limits.findPerTransferLimit("CC-AMEX")).thenReturn(null);

        TransferService service = new TransferService(accounts, mock(TransactionDao.class), transfers, limits);
        try {
            service.transfer("104000123455", "440000999990", new BigDecimal("10.00"), null, null, "dana.whitfield", true);
            fail("expected BankingException");
        } catch (BankingException e) {
            assertEquals("PRODUCT_LIMIT_NOT_CONFIGURED", e.getCode());
            assertEquals(422, e.getHttpStatus());
        }
        verifyZeroInteractions(transfers);
    }

    private static Account account(String number, String product) {
        Account a = new Account();
        a.setAccountNumber(number);
        a.setProductCode(product);
        a.setStatus("OPEN");
        a.setLedgerBalance(new BigDecimal("5000.00"));
        a.setHoldAmount(BigDecimal.ZERO);
        return a;
    }
}
