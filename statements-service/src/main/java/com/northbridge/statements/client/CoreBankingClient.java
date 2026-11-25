package com.northbridge.statements.client;

import com.northbridge.common.util.BankDates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

/** Reads account and posting data from core-banking over the internal service account. */
@Component
public class CoreBankingClient {

    private static final Logger log = LoggerFactory.getLogger(CoreBankingClient.class);

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${northbridge.core-banking.base-url:http://localhost:8080/api/v1}")
    private String baseUrl;

    @Value("${northbridge.core-banking.username:svc.payments}")
    private String username;

    @Value("${northbridge.core-banking.password:}")
    private String password;

    private HttpHeaders headers;

    @PostConstruct
    void init() {
        headers = new HttpHeaders();
        String basic = org.apache.commons.codec.binary.Base64.encodeBase64String(
                (username + ":" + password).getBytes(StandardCharsets.UTF_8));
        headers.set(HttpHeaders.AUTHORIZATION, "Basic " + basic);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getAccount(String accountNumber) {
        ResponseEntity<Map> response = restTemplate.exchange(baseUrl + "/accounts/" + accountNumber,
                HttpMethod.GET, new HttpEntity<Void>(headers), Map.class);
        return response.getBody();
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getTransactions(String accountNumber, Date from, Date to) {
        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/accounts/" + accountNumber + "/transactions")
                .queryParam("from", BankDates.formatDate(from))
                .queryParam("to", BankDates.formatDate(to))
                .queryParam("size", 500)
                .build().toUriString();
        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<Void>(headers), Map.class);
        Map<String, Object> body = response.getBody();
        if (body == null || body.get("content") == null) {
            log.warn("No postings returned for {}", accountNumber);
            return Collections.emptyList();
        }
        return (List<Map<String, Object>>) body.get("content");
    }
}
