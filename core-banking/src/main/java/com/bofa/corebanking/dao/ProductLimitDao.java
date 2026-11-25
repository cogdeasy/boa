package com.bofa.corebanking.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

/** Per-transfer limits keyed by destination product code ({@code product_limit}). */
@Repository
public class ProductLimitDao {

    private final JdbcTemplate jdbc;

    @Autowired
    public ProductLimitDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Returns the per-transfer limit for the product, or {@code null} if the product has no limit configured. */
    public BigDecimal findPerTransferLimit(String productCode) {
        List<BigDecimal> limits = jdbc.queryForList(
                "SELECT per_transfer_limit FROM product_limit WHERE product_code = ?", BigDecimal.class, productCode);
        return limits.isEmpty() ? null : limits.get(0);
    }
}
