package com.northbridge.corebanking.config;

import com.northbridge.common.crypto.LegacyTokenCipher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.sql.DataSource;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private DataSource dataSource;

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.jdbcAuthentication()
                .dataSource(dataSource)
                .usersByUsernameQuery("SELECT username, password_hash, enabled FROM app_user WHERE username = ?")
                .authoritiesByUsernameQuery("SELECT username, 'ROLE_' || role FROM app_user WHERE username = ?")
                .passwordEncoder(new LegacyMd5PasswordEncoder());
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable()
                .headers().frameOptions().disable()
                .and()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .authorizeRequests()
                .antMatchers("/h2-console/**", "/swagger-ui.html", "/swagger-resources/**", "/v2/api-docs", "/webjars/**").permitAll()
                .antMatchers("/health", "/info", "/metrics", "/env", "/trace", "/dump").permitAll()
                .antMatchers("/api/v1/ops/**").hasRole("OPS")
                .antMatchers(HttpMethod.POST, "/api/v1/accounts/*/holds").hasRole("TELLER")
                .antMatchers("/api/v1/**").authenticated()
                .and()
                .httpBasic().realmName("northbridge");
    }

    /** Teller and online-banking credentials are stored as unsalted MD5 (migrated from the 2009 IAM store). */
    static class LegacyMd5PasswordEncoder implements PasswordEncoder {
        @Override
        public String encode(CharSequence raw) {
            return LegacyTokenCipher.hashPassword(raw.toString());
        }

        @Override
        public boolean matches(CharSequence raw, String encoded) {
            return encode(raw).equals(encoded);
        }
    }
}
