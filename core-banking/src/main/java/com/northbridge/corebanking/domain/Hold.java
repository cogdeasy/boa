package com.northbridge.corebanking.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.math.BigDecimal;
import java.util.Date;

/** Funds hold (deposit hold, legal hold, card pre-auth) reducing available balance. */
public class Hold {

    private long holdId;
    private String accountNumber;
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal amount;
    private String reason;
    private String placedBy;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "America/New_York")
    private Date releaseOn;
    private String status;

    public long getHoldId() { return holdId; }
    public void setHoldId(long holdId) { this.holdId = holdId; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getPlacedBy() { return placedBy; }
    public void setPlacedBy(String placedBy) { this.placedBy = placedBy; }
    public Date getReleaseOn() { return releaseOn; }
    public void setReleaseOn(Date releaseOn) { this.releaseOn = releaseOn; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
