package com.bofa.corebanking.dao;

import com.bofa.corebanking.domain.Account;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class AccountDao {

    private static final String SELECT = "SELECT a.account_number, a.cif, a.product_code, a.nickname, a.status, a.ledger_balance, "
            + "a.daily_transfer_limit, a.opened_on, "
            + "(SELECT COALESCE(SUM(h.amount), 0) FROM funds_hold h WHERE h.account_number = a.account_number AND h.status = 'ACTIVE') AS hold_amount "
            + "FROM account a";

    private final JdbcTemplate jdbc;

    @Autowired
    public AccountDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Account findByNumber(String accountNumber) {
        try {
            return jdbc.queryForObject(SELECT + " WHERE a.account_number = ?", MAPPER, accountNumber);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    public List<Account> findByCif(String cif) {
        return jdbc.query(SELECT + " WHERE a.cif = ? ORDER BY a.product_code, a.account_number", MAPPER, cif);
    }

    public List<Account> findOpenDeposits() {
        return jdbc.query(SELECT + " WHERE a.status = 'OPEN' AND a.product_code LIKE 'DDA-%'", MAPPER);
    }

    public void updateLedgerBalance(String accountNumber, BigDecimal newBalance) {
        jdbc.update("UPDATE account SET ledger_balance = ? WHERE account_number = ?", newBalance, accountNumber);
    }

    private static final RowMapper<Account> MAPPER = new RowMapper<Account>() {
        @Override
        public Account mapRow(ResultSet rs, int rowNum) throws SQLException {
            Account a = new Account();
            a.setAccountNumber(rs.getString("account_number"));
            a.setCif(rs.getString("cif"));
            a.setProductCode(rs.getString("product_code"));
            a.setNickname(rs.getString("nickname"));
            a.setStatus(rs.getString("status"));
            a.setLedgerBalance(rs.getBigDecimal("ledger_balance"));
            a.setHoldAmount(rs.getBigDecimal("hold_amount"));
            a.setDailyTransferLimit(rs.getBigDecimal("daily_transfer_limit"));
            a.setOpenedOn(rs.getDate("opened_on"));
            return a;
        }
    };
}
