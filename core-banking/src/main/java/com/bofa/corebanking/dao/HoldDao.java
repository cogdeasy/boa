package com.bofa.corebanking.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.util.Date;

@Repository
public class HoldDao {

    private final JdbcTemplate jdbc;

    @Autowired
    public HoldDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long insert(final String accountNumber, final BigDecimal amount, final String reason, final String placedBy, final Date releaseOn) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO funds_hold (account_number, amount, reason, placed_by, release_on, status) VALUES (?, ?, ?, ?, ?, 'ACTIVE')",
                    new String[]{"hold_id"});
            ps.setString(1, accountNumber);
            ps.setBigDecimal(2, amount);
            ps.setString(3, reason);
            ps.setString(4, placedBy);
            ps.setDate(5, new java.sql.Date(releaseOn.getTime()));
            return ps;
        }, keys);
        return keys.getKey().longValue();
    }

    public int releaseExpired(Date businessDate) {
        return jdbc.update("UPDATE funds_hold SET status = 'RELEASED' WHERE status = 'ACTIVE' AND release_on <= ?",
                new java.sql.Date(businessDate.getTime()));
    }
}
