package com.bofa.corebanking;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Map;

import static org.junit.Assert.assertEquals;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "bofa.seed.transactions-per-account=200")
public class TransferApiTest {

    @Autowired
    private TestRestTemplate rest;

    @Test
    public void customerCanMoveMoneyBetweenOwnAccounts() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"fromAccount\":\"104000123455\",\"toAccount\":\"104000123463\",\"amount\":\"10.00\",\"memo\":\"test\"}";
        ResponseEntity<Map> response = rest.withBasicAuth("dana.whitfield", "Welcome1!")
                .postForEntity("/api/v1/transfers", new HttpEntity<String>(body, headers), Map.class);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("POSTED", response.getBody().get("status"));
    }

    @Test
    public void cardPaymentUnderPerTransferLimitPosts() {
        ResponseEntity<Map> response = transfer("104000123463", "440000528719", "250.00");
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("POSTED", response.getBody().get("status"));
    }

    @Test
    public void cardPaymentOverPerTransferLimitIsRejected() {
        ResponseEntity<Map> response = transfer("104000123463", "440000528719", "15000.01");
        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
        assertEquals("LIMIT_EXCEEDED", response.getBody().get("code"));
        assertEquals("Amount exceeds the per-transfer limit of 15000.00 for CC-VISA", response.getBody().get("message"));
    }

    private ResponseEntity<Map> transfer(String from, String to, String amount) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"fromAccount\":\"" + from + "\",\"toAccount\":\"" + to + "\",\"amount\":\"" + amount + "\"}";
        return rest.withBasicAuth("dana.whitfield", "Welcome1!")
                .postForEntity("/api/v1/transfers", new HttpEntity<String>(body, headers), Map.class);
    }

    @Test
    public void customerCannotReadAnotherCustomersProfile() {
        ResponseEntity<String> response = rest.withBasicAuth("dana.whitfield", "Welcome1!")
                .getForEntity("/api/v1/customers/100000002", String.class);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }

    @Test
    public void customerResolvesOwnProfile() {
        ResponseEntity<Map> response = rest.withBasicAuth("dana.whitfield", "Welcome1!")
                .getForEntity("/api/v1/customers/me", Map.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("100000001", response.getBody().get("cif"));
    }

    @Test
    public void tellerCannotUseCustomerSelfEndpoint() {
        ResponseEntity<String> response = rest.withBasicAuth("teller01", "Teller#2019")
                .getForEntity("/api/v1/customers/me", String.class);
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
    }
}
