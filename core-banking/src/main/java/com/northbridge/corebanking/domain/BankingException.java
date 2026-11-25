package com.northbridge.corebanking.domain;

/** Business rule violation with a stable error code returned to channels (NSF, LIMIT_EXCEEDED, ...). */
public class BankingException extends RuntimeException {

    private final String code;
    private final int httpStatus;

    public BankingException(String code, int httpStatus, String message) {
        super(message);
        this.code = code;
        this.httpStatus = httpStatus;
    }

    public static BankingException notFound(String what) {
        return new BankingException("ACCOUNT_NOT_FOUND", 404, what + " not found");
    }

    public String getCode() {
        return code;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
