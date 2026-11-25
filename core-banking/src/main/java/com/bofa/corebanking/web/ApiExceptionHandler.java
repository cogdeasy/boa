package com.bofa.corebanking.web;

import com.bofa.corebanking.domain.BankingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Map;

@ControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(BankingException.class)
    public ResponseEntity<Map<String, Object>> banking(BankingException e) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("code", e.getCode());
        body.put("message", e.getMessage());
        return ResponseEntity.status(e.getHttpStatus()).body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> denied(AccessDeniedException e) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("code", "FORBIDDEN");
        body.put("message", "Access is denied");
        return ResponseEntity.status(403).body(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> badRequest(IllegalArgumentException e) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("code", "BAD_REQUEST");
        body.put("message", e.getMessage());
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> unexpected(Exception e) {
        log.error("Unhandled error", e);
        StringWriter trace = new StringWriter();
        e.printStackTrace(new PrintWriter(trace));
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("code", "INTERNAL_ERROR");
        body.put("message", e.toString());
        body.put("detail", trace.toString());
        return ResponseEntity.status(500).body(body);
    }
}
