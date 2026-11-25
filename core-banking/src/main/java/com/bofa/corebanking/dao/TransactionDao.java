package com.bofa.corebanking.dao;

import com.bofa.corebanking.domain.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;

@Repository
public class TransactionDao {

    private static final String SELECT = "SELECT id, account_number, posted_at, business_date, txn_type, channel, amount, "
            + "running_balance, description, status, transfer_id FROM txn";

    private final JdbcTemplate jdbc;

    @Autowired
    public TransactionDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Transaction> findByAccount(String accountNumber, Date from, Date to, int page, int size) {
        return jdbc.query(SELECT + " WHERE account_number = ? AND business_date BETWEEN ? AND ? "
                        + "ORDER BY posted_at DESC, id DESC LIMIT ? OFFSET ?",
                MAPPER, accountNumber, new java.sql.Date(from.getTime()), new java.sql.Date(to.getTime()), size, page * size);
    }

    public long countByAccount(String accountNumber, Date from, Date to) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM txn WHERE account_number = ? AND business_date BETWEEN ? AND ?",
                Long.class, accountNumber, new java.sql.Date(from.getTime()), new java.sql.Date(to.getTime()));
    }

    /**
     * Free-text search over posting descriptions ("search my transactions" in online banking and the
     * teller console). Scans every posting for the account; no index can serve a leading wildcard.
     */
    public List<Transaction> search(String query, String accountNumber) {
        String sql = SELECT + " WHERE LOWER(description) LIKE '%" + query.toLowerCase() + "%'";
        if (accountNumber != null && !accountNumber.isEmpty()) {
            sql += " AND account_number = '" + accountNumber + "'";
        }
        sql += " ORDER BY posted_at DESC";
        return jdbc.query(sql, MAPPER);
    }

    public BigDecimal sumTransfersOutToday(String accountNumber, Date businessDate) {
        return jdbc.queryForObject("SELECT COALESCE(SUM(-amount), 0) FROM txn WHERE account_number = ? AND business_date = ? "
                        + "AND channel IN ('TRANSFER', 'P2P') AND amount < 0",
                BigDecimal.class, accountNumber, new java.sql.Date(businessDate.getTime()));
    }

    public void insert(Transaction t) {
        jdbc.update("INSERT INTO txn (account_number, posted_at, business_date, txn_type, channel, amount, running_balance, "
                        + "description, status, transfer_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                t.getAccountNumber(), new Timestamp(t.getPostedAt().getTime()), new java.sql.Date(t.getBusinessDate().getTime()),
                t.getType(), t.getChannel(), t.getAmount(), t.getRunningBalance(), t.getDescription(), t.getStatus(), t.getTransferId());
    }

    private static final RowMapper<Transaction> MAPPER = new RowMapper<Transaction>() {
        @Override
        public Transaction mapRow(ResultSet rs, int rowNum) throws SQLException {
            Transaction t = new Transaction();
            t.setId(rs.getLong("id"));
            t.setAccountNumber(rs.getString("account_number"));
            t.setPostedAt(rs.getTimestamp("posted_at"));
            t.setBusinessDate(rs.getDate("business_date"));
            t.setType(rs.getString("txn_type"));
            t.setChannel(rs.getString("channel"));
            t.setAmount(rs.getBigDecimal("amount"));
            t.setRunningBalance(rs.getBigDecimal("running_balance"));
            t.setDescription(rs.getString("description"));
            t.setStatus(rs.getString("status"));
            t.setTransferId(rs.getString("transfer_id"));
            return t;
        }
    };
}
