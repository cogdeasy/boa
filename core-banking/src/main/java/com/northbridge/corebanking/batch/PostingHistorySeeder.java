package com.northbridge.corebanking.batch;

import com.northbridge.common.util.BankDates;
import com.northbridge.corebanking.dao.AccountDao;
import com.northbridge.corebanking.domain.Account;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Random;

/**
 * Loads a representative posting history into the in-memory database used by the local and CI
 * profiles, so that pagination, statements and transaction search behave like production volumes.
 * Against Oracle (profile {@code oracle}) this is disabled and the real history is used.
 */
@Component
public class PostingHistorySeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PostingHistorySeeder.class);

    private static final String[] MERCHANTS = {
            "WHOLE FDS MKT #10233", "SHELL OIL 574120", "NETFLIX.COM", "CON EDISON BILLPAY", "MTA*NYCT PAYGO",
            "AMAZON MKTPL US*2K41", "STARBUCKS STORE 7781", "TRADER JOE'S #542", "DELTA AIR 0062419",
            "VERIZON WIRELESS PMT", "CVS/PHARMACY #04821", "UBER TRIP HELP.UBER", "APPLE.COM/BILL",
            "SQ *BLUE BOTTLE COFFEE", "WHOLESALE CLUB #882", "CHIPOTLE 2188", "LYFT *RIDE THU 6PM",
            "PELOTON MEMBERSHIP", "NY STATE DMV ONLINE", "HOME DEPOT #6412"
    };
    private static final String[] CHANNELS = {"CARD", "CARD", "CARD", "ACH", "BILLPAY", "ATM", "P2P", "BRANCH"};
    private static final String[] CREDITS = {"PAYROLL DEP NORTHWIND LLC", "ACH CREDIT IRS TREAS 310", "ZELLE FROM J MARTINEZ",
            "MOBILE CHECK DEPOSIT", "INTEREST PAYMENT", "REFUND AMAZON MKTPL"};

    private static final int HISTORY_BUSINESS_DAYS = 2190;

    private final JdbcTemplate jdbc;
    private final AccountDao accountDao;

    @Value("${northbridge.seed.enabled:false}")
    private boolean enabled;

    @Value("${northbridge.seed.transactions-per-account:60000}")
    private int perAccount;

    @Autowired
    public PostingHistorySeeder(JdbcTemplate jdbc, AccountDao accountDao) {
        this.jdbc = jdbc;
        this.accountDao = accountDao;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        long existing = jdbc.queryForObject("SELECT COUNT(*) FROM txn", Long.class);
        if (existing > 0) {
            return;
        }
        long start = System.currentTimeMillis();
        List<Account> accounts = accountDao.findOpenDeposits();
        for (Account account : accounts) {
            seed(account);
        }
        log.info("Seeded posting history for {} accounts in {} ms", accounts.size(), System.currentTimeMillis() - start);
    }

    private void seed(final Account account) {
        final Random random = new Random(account.getAccountNumber().hashCode());
        final boolean savings = "DDA-SAV".equals(account.getProductCode());
        final Date today = BankDates.businessDate(new Date());
        final int count = savings ? perAccount / 10 : perAccount;
        final int perDay = Math.max(1, count / HISTORY_BUSINESS_DAYS);
        // postings are generated newest-first, walking the balance backwards from today's ledger balance
        final BigDecimal[] balance = {account.getLedgerBalance()};

        jdbc.execute("SET REFERENTIAL_INTEGRITY FALSE");
        try {
            for (int offset = 0; offset < count; offset += 1000) {
                final int batchStart = offset;
                final int batchSize = Math.min(1000, count - offset);
                jdbc.batchUpdate("INSERT INTO txn (account_number, posted_at, business_date, txn_type, channel, amount, "
                        + "running_balance, description, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'POSTED')",
                        new org.springframework.jdbc.core.BatchPreparedStatementSetter() {
                            @Override
                            public void setValues(PreparedStatement ps, int i) throws SQLException {
                                int n = batchStart + i;
                                Date businessDate = BankDates.addDays(today, -(n / perDay) - 1);
                                long postedAt = businessDate.getTime() + (9 + random.nextInt(10)) * 3600000L + random.nextInt(3600000);
                                BigDecimal after = balance[0];
                                boolean credit = after.compareTo(new BigDecimal(savings ? 30000 : 9000)) > 0
                                        || (after.compareTo(new BigDecimal(1500)) > 0 && random.nextInt(savings ? 4 : 12) == 0);
                                BigDecimal amount = credit
                                        ? new BigDecimal(200 + random.nextInt(Math.max(1, after.intValue() - 400) * 100 / 2)).movePointLeft(2)
                                        : new BigDecimal(-(150 + random.nextInt(18000))).movePointLeft(2);
                                balance[0] = after.subtract(amount);
                                String description = credit ? CREDITS[random.nextInt(CREDITS.length)]
                                        : MERCHANTS[random.nextInt(MERCHANTS.length)];
                                ps.setString(1, account.getAccountNumber());
                                ps.setTimestamp(2, new Timestamp(postedAt));
                                ps.setDate(3, new java.sql.Date(businessDate.getTime()));
                                ps.setString(4, credit ? "CREDIT" : "DEBIT");
                                ps.setString(5, credit ? "ACH" : CHANNELS[random.nextInt(CHANNELS.length)]);
                                ps.setBigDecimal(6, amount);
                                ps.setBigDecimal(7, after);
                                ps.setString(8, description);
                            }

                            @Override
                            public int getBatchSize() {
                                return batchSize;
                            }
                        });
            }
        } finally {
            jdbc.execute("SET REFERENTIAL_INTEGRITY TRUE");
        }
    }
}
