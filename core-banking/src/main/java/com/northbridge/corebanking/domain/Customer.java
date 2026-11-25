package com.northbridge.corebanking.domain;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;

/** CIF record (Customer Information File). */
public class Customer {

    private String cif;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String ssnLast4;
    private String segment;
    private String kycStatus;
    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "America/New_York")
    private Date customerSince;

    public String getCif() { return cif; }
    public void setCif(String cif) { this.cif = cif; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getSsnLast4() { return ssnLast4; }
    public void setSsnLast4(String ssnLast4) { this.ssnLast4 = ssnLast4; }
    public String getSegment() { return segment; }
    public void setSegment(String segment) { this.segment = segment; }
    public String getKycStatus() { return kycStatus; }
    public void setKycStatus(String kycStatus) { this.kycStatus = kycStatus; }
    public Date getCustomerSince() { return customerSince; }
    public void setCustomerSince(Date customerSince) { this.customerSince = customerSince; }
}
