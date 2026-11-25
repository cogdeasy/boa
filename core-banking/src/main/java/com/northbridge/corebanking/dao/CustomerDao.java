package com.northbridge.corebanking.dao;

import com.northbridge.corebanking.domain.Customer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
public class CustomerDao {

    private static final String SELECT = "SELECT cif, first_name, last_name, email, phone, ssn_last4, segment, kyc_status, customer_since FROM customer";

    private final JdbcTemplate jdbc;

    @Autowired
    public CustomerDao(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Customer findByCif(String cif) {
        try {
            return jdbc.queryForObject(SELECT + " WHERE cif = ?", MAPPER, cif);
        } catch (EmptyResultDataAccessException e) {
            return null;
        }
    }

    /** Branch look-up by surname, used by the teller console "Find customer" screen. */
    public List<Customer> searchByLastName(String lastName) {
        String sql = SELECT + " WHERE UPPER(last_name) LIKE UPPER('" + lastName + "%') ORDER BY last_name, first_name";
        return jdbc.query(sql, MAPPER);
    }

    private static final RowMapper<Customer> MAPPER = new RowMapper<Customer>() {
        @Override
        public Customer mapRow(ResultSet rs, int rowNum) throws SQLException {
            Customer c = new Customer();
            c.setCif(rs.getString("cif"));
            c.setFirstName(rs.getString("first_name"));
            c.setLastName(rs.getString("last_name"));
            c.setEmail(rs.getString("email"));
            c.setPhone(rs.getString("phone"));
            c.setSsnLast4(rs.getString("ssn_last4"));
            c.setSegment(rs.getString("segment"));
            c.setKycStatus(rs.getString("kyc_status"));
            c.setCustomerSince(rs.getDate("customer_since"));
            return c;
        }
    };
}
