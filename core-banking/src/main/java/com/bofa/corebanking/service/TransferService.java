package com.bofa.corebanking.service;

import com.bofa.common.util.AuditLogger;
import com.bofa.common.util.BankDates;
import com.bofa.corebanking.dao.AccountDao;
import com.bofa.corebanking.dao.ProductLimitDao;
import com.bofa.corebanking.dao.TransactionDao;
import com.bofa.corebanking.dao.TransferDao;
import com.bofa.corebanking.domain.Account;
import com.bofa.corebanking.domain.BankingException;
import com.bofa.corebanking.domain.Transaction;
import com.bofa.corebanking.domain.Transfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Book transfers between Bank of America accounts (PSTG-BT). Also the posting entry point used by the
 * payments hub, which moves customer funds into the GL-SUSP suspense account before settlement.
 */
@Service
public class TransferService {

    private static final Logger log = LoggerFactory.getLogger(TransferService.class);
    private static final SimpleDateFormat ID_DATE = new SimpleDateFormat("yyyyMMdd");

    private final AccountDao accountDao;
    private final TransactionDao transactionDao;
    private final TransferDao transferDao;
    private final ProductLimitDao productLimitDao;

    @Autowired
    public TransferService(AccountDao accountDao, TransactionDao transactionDao, TransferDao transferDao,
                           ProductLimitDao productLimitDao) {
        this.accountDao = accountDao;
        this.transactionDao = transactionDao;
        this.transferDao = transferDao;
        this.productLimitDao = productLimitDao;
    }

    @Transactional
    public Transfer transfer(String fromNumber, String toNumber, BigDecimal amount, String memo, String idempotencyKey,
                             String initiatedBy, boolean enforceDailyLimit) {
        log.info("Transfer request from={} to={} amount={} by={}", fromNumber, toNumber, amount, initiatedBy);

        if (idempotencyKey != null) {
            Transfer existing = transferDao.findByIdempotencyKey(idempotencyKey);
            if (existing != null) {
                throw new BankingException("DUPLICATE", 409, "Transfer already submitted as " + existing.getTransferId());
            }
        }
        if (amount == null || amount.signum() <= 0) {
            throw new BankingException("INVALID_AMOUNT", 400, "Amount must be positive");
        }
        if (fromNumber.equals(toNumber)) {
            throw new BankingException("SAME_ACCOUNT", 422, "From and to accounts must differ");
        }

        Account from = accountDao.findByNumber(fromNumber);
        Account to = accountDao.findByNumber(toNumber);
        if (from == null || to == null) {
            throw BankingException.notFound("Account " + (from == null ? fromNumber : toNumber));
        }
        if (!from.isOpen() || !to.isOpen()) {
            throw new BankingException("ACCOUNT_NOT_OPEN", 422, "Both accounts must be open");
        }
        checkPerTransferLimit(to, amount);

        Date now = new Date();
        Date businessDate = BankDates.businessDate(now);

        if (!from.isGeneralLedger() && from.getAvailableBalance().compareTo(amount) < 0) {
            log.warn("NSF on transfer from={} available={} requested={}", fromNumber, from.getAvailableBalance(), amount);
            throw new BankingException("NSF", 422, "Insufficient available funds");
        }
        if (enforceDailyLimit && from.getDailyTransferLimit() != null) {
            BigDecimal usedToday = transactionDao.sumTransfersOutToday(fromNumber, businessDate);
            if (usedToday.add(amount).compareTo(from.getDailyTransferLimit()) > 0) {
                throw new BankingException("LIMIT_EXCEEDED", 422, "Daily transfer limit of " + from.getDailyTransferLimit() + " exceeded");
            }
        }

        BigDecimal fromBalance = from.getLedgerBalance().subtract(amount);
        BigDecimal toBalance = to.getLedgerBalance().add(amount);
        accountDao.updateLedgerBalance(fromNumber, fromBalance);
        accountDao.updateLedgerBalance(toNumber, toBalance);

        Transfer t = new Transfer();
        t.setTransferId("TRF-" + ID_DATE.format(now) + "-" + String.format("%06d", transferDao.nextSequence()));
        t.setFromAccount(fromNumber);
        t.setToAccount(toNumber);
        t.setAmount(amount);
        t.setMemo(memo);
        t.setIdempotencyKey(idempotencyKey);
        t.setStatus("POSTED");
        t.setInitiatedBy(initiatedBy);
        t.setPostedAt(now);
        t.setBusinessDate(businessDate);
        transferDao.insert(t);

        String description = memo == null || memo.isEmpty() ? "ONLINE TRANSFER" : "ONLINE TRANSFER " + memo.toUpperCase();
        transactionDao.insert(posting(fromNumber, now, businessDate, "DEBIT", amount.negate(), fromBalance,
                description + " TO " + to.getMaskedNumber(), t.getTransferId()));
        transactionDao.insert(posting(toNumber, now, businessDate, "CREDIT", amount, toBalance,
                description + " FROM " + from.getMaskedNumber(), t.getTransferId()));

        AuditLogger.record("AUD-TRF-POSTED", initiatedBy, "id=" + t.getTransferId() + " from=" + fromNumber + " to=" + toNumber + " amount=" + amount);
        log.info("Transfer {} posted from={} newBalance={} to={} newBalance={}", t.getTransferId(), fromNumber, fromBalance, toNumber, toBalance);
        return t;
    }

    /**
     * Enforces the per-transfer limit of the destination product. Internal GL postings (payments hub into GL-SUSP)
     * are exempt. A customer product with no configured limit fails closed.
     */
    private void checkPerTransferLimit(Account to, BigDecimal amount) {
        if (to.isGeneralLedger()) {
            return;
        }
        String productCode = to.getProductCode();
        BigDecimal limit = productCode == null ? null : productLimitDao.findPerTransferLimit(productCode);
        if (limit == null) {
            log.error("No per-transfer limit configured for product={} to={}", productCode, to.getMaskedNumber());
            throw new BankingException("LIMIT_NOT_CONFIGURED", 422,
                    "Transfers to product " + productCode + " are not permitted: no per-transfer limit is configured");
        }
        if (amount.compareTo(limit) > 0) {
            throw new BankingException("LIMIT_EXCEEDED", 422,
                    "Per-transfer limit of " + limit + " for " + productCode + " exceeded");
        }
    }

    public Transfer get(String transferId) {
        Transfer t = transferDao.findById(transferId);
        if (t == null) {
            throw new BankingException("TRANSFER_NOT_FOUND", 404, "Transfer " + transferId + " not found");
        }
        return t;
    }

    private static Transaction posting(String account, Date now, Date businessDate, String type, BigDecimal amount,
                                       BigDecimal runningBalance, String description, String transferId) {
        Transaction tx = new Transaction();
        tx.setAccountNumber(account);
        tx.setPostedAt(now);
        tx.setBusinessDate(businessDate);
        tx.setType(type);
        tx.setChannel("TRANSFER");
        tx.setAmount(amount);
        tx.setRunningBalance(runningBalance);
        tx.setDescription(description);
        tx.setStatus("POSTED");
        tx.setTransferId(transferId);
        return tx;
    }
}
