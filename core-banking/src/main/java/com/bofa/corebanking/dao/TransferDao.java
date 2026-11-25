package com.bofa.corebanking.dao;

import com.bofa.corebanking.domain.Transfer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

@Repository
public class TransferDao {

    private static final String SELECT = "SELECT transfer_id, from_account, to_account, amount, memo, idempotency_key, status, "
            + "initiated_by, posted_at, business_date FROM book_transfer";

    private final JdbcTemplate jdbc;

    @Autowired
    public TransferDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Transfer findById(String transferId) {
        try {
            return jdbc.queryForObject(SELECT + " WHERE transfer_id = ?", MAPPER, transferId);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public Transfer findByIdempotencyKey(String key) {
        try {
            return jdbc.queryForObject(SELECT + " WHERE idempotency_key = ?", MAPPER, key);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public long nextSequence() {
        return jdbc.queryForObject("SELECT NEXT VALUE FOR transfer_seq", Long.class);
    }

    public void insert(Transfer t) {
        jdbc.update("INSERT INTO book_transfer (transfer_id, from_account, to_account, amount, memo, idempotency_key, status, "
                        + "initiated_by, posted_at, business_date) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                t.getTransferId(), t.getFromAccount(), t.getToAccount(), t.getAmount(), t.getMemo(), t.getIdempotencyKey(),
                t.getStatus(), t.getInitiatedBy(), new Timestamp(t.getPostedAt().getTime()),
                new java.sql.Date(t.getBusinessDate().getTime()));
    }

    private static final RowMapper<Transfer> MAPPER = new RowMapper<Transfer>() {
        @Override
        public Transfer mapRow(ResultSet rs, int rowNum) throws SQLException {
            Transfer t = new Transfer();
            t.setTransferId(rs.getString("transfer_id"));
            t.setFromAccount(rs.getString("from_account"));
            t.setToAccount(rs.getString("to_account"));
            t.setAmount(rs.getBigDecimal("amount"));
            t.setMemo(rs.getString("memo"));
            t.setIdempotencyKey(rs.getString("idempotency_key"));
            t.setStatus(rs.getString("status"));
            t.setInitiatedBy(rs.getString("initiated_by"));
            t.setPostedAt(rs.getTimestamp("posted_at"));
            t.setBusinessDate(rs.getDate("business_date"));
            return t;
        }
    };
}
