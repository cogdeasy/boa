package com.northbridge.corebanking.batch;

import com.northbridge.common.util.BankDates;
import com.northbridge.corebanking.dao.AccountDao;
import com.northbridge.corebanking.dao.HoldDao;
import com.northbridge.corebanking.dao.TransactionDao;
import com.northbridge.corebanking.domain.Account;
import com.northbridge.corebanking.domain.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

/**
 * EOD batch: releases expired holds and accrues daily interest on savings (DDA-SAV) at the posted APY.
 * Runs at the 20:00 ET cut-off.
 */
@Component
public class EndOfDayJob {

    private static final Logger log = LoggerFactory.getLogger(EndOfDayJob.class);
    private static final BigDecimal DAYS_IN_YEAR = new BigDecimal("365");

    private final AccountDao accountDao;
    private final HoldDao holdDao;
    private final TransactionDao transactionDao;

    @Value("${northbridge.eod.savings-apy:0.0425}")
    private BigDecimal savingsApy;

    @Autowired
    public EndOfDayJob(AccountDao accountDao, HoldDao holdDao, TransactionDao transactionDao) {
        this.accountDao = accountDao;
        this.holdDao = holdDao;
        this.transactionDao = transactionDao;
    }

    @Scheduled(cron = "${northbridge.eod.cron:0 0 20 * * MON-FRI}", zone = "America/New_York")
    @Transactional
    public void run() {
        Date businessDate = BankDates.businessDate(new Date());
        log.info("EOD start businessDate={}", BankDates.formatDate(businessDate));
        int released = holdDao.releaseExpired(businessDate);
        int accrued = 0;
        for (Account a : accountDao.findOpenDeposits()) {
            if (!"DDA-SAV".equals(a.getProductCode()) || a.getLedgerBalance().signum() <= 0) {
                continue;
            }
            BigDecimal interest = a.getLedgerBalance().multiply(savingsApy).divide(DAYS_IN_YEAR, 2, RoundingMode.HALF_EVEN);
            if (interest.signum() == 0) {
                continue;
            }
            BigDecimal newBalance = a.getLedgerBalance().add(interest);
            accountDao.updateLedgerBalance(a.getAccountNumber(), newBalance);
            Transaction tx = new Transaction();
            tx.setAccountNumber(a.getAccountNumber());
            tx.setPostedAt(new Date());
            tx.setBusinessDate(businessDate);
            tx.setType("CREDIT");
            tx.setChannel("INTEREST");
            tx.setAmount(interest);
            tx.setRunningBalance(newBalance);
            tx.setDescription("INTEREST PAYMENT");
            tx.setStatus("POSTED");
            transactionDao.insert(tx);
            accrued++;
        }
        log.info("EOD complete holdsReleased={} interestPostings={}", released, accrued);
    }
}
