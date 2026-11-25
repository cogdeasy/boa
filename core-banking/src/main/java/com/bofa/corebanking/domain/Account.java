package com.bofa.corebanking.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.bofa.common.util.AccountNumbers;

import java.math.BigDecimal;
import java.util.Date;

/** Demand deposit account (DDA-CHK, DDA-SAV) or an internal GL account (GL-*). */
public class Account {

    private String accountNumber;
    private String cif;
    private String productCode;
    private String nickname;
    private String status;
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal ledgerBalance;
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal holdAmount;
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal dailyTransferLimit;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "America/New_York")
    private Date openedOn;

    public String getMaskedNumber() {
        return AccountNumbers.mask(accountNumber);
    }

    @JsonSerialize(using = ToStringSerializer.class)
    public BigDecimal getAvailableBalance() {
        return ledgerBalance.subtract(holdAmount == null ? BigDecimal.ZERO : holdAmount);
    }

    public boolean isOpen() {
        return "OPEN".equals(status);
    }

    public boolean isGeneralLedger() {
        return productCode != null && productCode.startsWith("GL-");
    }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public String getCif() { return cif; }
    public void setCif(String cif) { this.cif = cif; }
    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getLedgerBalance() { return ledgerBalance; }
    public void setLedgerBalance(BigDecimal ledgerBalance) { this.ledgerBalance = ledgerBalance; }
    public BigDecimal getHoldAmount() { return holdAmount; }
    public void setHoldAmount(BigDecimal holdAmount) { this.holdAmount = holdAmount; }
    public BigDecimal getDailyTransferLimit() { return dailyTransferLimit; }
    public void setDailyTransferLimit(BigDecimal dailyTransferLimit) { this.dailyTransferLimit = dailyTransferLimit; }
    public Date getOpenedOn() { return openedOn; }
    public void setOpenedOn(Date openedOn) { this.openedOn = openedOn; }
}
