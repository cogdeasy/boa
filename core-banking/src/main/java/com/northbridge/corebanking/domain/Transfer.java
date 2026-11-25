package com.northbridge.corebanking.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.math.BigDecimal;
import java.util.Date;

/** Internal book transfer between two Northbridge accounts. */
public class Transfer {

    private String transferId;
    private String fromAccount;
    private String toAccount;
    @JsonSerialize(using = ToStringSerializer.class)
    private BigDecimal amount;
    private String memo;
    private String idempotencyKey;
    private String status;
    private String initiatedBy;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss", timezone = "America/New_York")
    private Date postedAt;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "America/New_York")
    private Date businessDate;

    public String getTransferId() { return transferId; }
    public void setTransferId(String transferId) { this.transferId = transferId; }
    public String getFromAccount() { return fromAccount; }
    public void setFromAccount(String fromAccount) { this.fromAccount = fromAccount; }
    public String getToAccount() { return toAccount; }
    public void setToAccount(String toAccount) { this.toAccount = toAccount; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getMemo() { return memo; }
    public void setMemo(String memo) { this.memo = memo; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getInitiatedBy() { return initiatedBy; }
    public void setInitiatedBy(String initiatedBy) { this.initiatedBy = initiatedBy; }
    public Date getPostedAt() { return postedAt; }
    public void setPostedAt(Date postedAt) { this.postedAt = postedAt; }
    public Date getBusinessDate() { return businessDate; }
    public void setBusinessDate(Date businessDate) { this.businessDate = businessDate; }
}
