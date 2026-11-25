package com.northbridge.corebanking.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.math.BigDecimal;
import java.util.Date;

/** A single posting against a DDA or GL account. Debits carry a negative amount. */
public class Transaction {

    private long id;
    private String accountNumber;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "America/New_York")
    private Date postedAt;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "America/New_York")
    private Date businessDate;
    private String type;
    private String channel;
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal amount;
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal runningBalance;
    private String description;
    private String status;
    private String transferId;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public Date getPostedAt() { return postedAt; }
    public void setPostedAt(Date postedAt) { this.postedAt = postedAt; }
    public Date getBusinessDate() { return businessDate; }
    public void setBusinessDate(Date businessDate) { this.businessDate = businessDate; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getRunningBalance() { return runningBalance; }
    public void setRunningBalance(BigDecimal runningBalance) { this.runningBalance = runningBalance; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getTransferId() { return transferId; }
    public void setTransferId(String transferId) { this.transferId = transferId; }
}
