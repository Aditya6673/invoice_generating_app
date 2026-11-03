package com.rishav.billing.auth.entity;

import com.rishav.billing.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "business")
public class Business extends BaseEntity {

    @Column(name = "name", nullable = false, columnDefinition = "TEXT")
    private String name;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "gstin", length = 15)
    private String gstin;

    @Column(name = "state_code", length = 2)
    private String stateCode;

    @Column(name = "logo_url", columnDefinition = "TEXT")
    private String logoUrl;

    @Column(name = "invoice_prefix", length = 10, nullable = false)
    private String invoicePrefix = "INV";

    @Column(name = "bank_name", columnDefinition = "TEXT")
    private String bankName;

    @Column(name = "bank_account", columnDefinition = "TEXT")
    private String bankAccount;

    @Column(name = "bank_ifsc", length = 11)
    private String bankIfsc;

    @Column(name = "default_payment_terms")
    private Integer defaultPaymentTerms = 30;

    public Business() {
    }

    public Business(String name, String gstin, String stateCode) {
        this.name = name;
        this.gstin = gstin;
        this.stateCode = stateCode;
        this.invoicePrefix = "INV";
        this.defaultPaymentTerms = 30;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getGstin() {
        return gstin;
    }

    public void setGstin(String gstin) {
        this.gstin = gstin;
    }

    public String getStateCode() {
        return stateCode;
    }

    public void setStateCode(String stateCode) {
        this.stateCode = stateCode;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getInvoicePrefix() {
        return invoicePrefix;
    }

    public void setInvoicePrefix(String invoicePrefix) {
        this.invoicePrefix = invoicePrefix;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getBankAccount() {
        return bankAccount;
    }

    public void setBankAccount(String bankAccount) {
        this.bankAccount = bankAccount;
    }

    public String getBankIfsc() {
        return bankIfsc;
    }

    public void setBankIfsc(String bankIfsc) {
        this.bankIfsc = bankIfsc;
    }

    public Integer getDefaultPaymentTerms() {
        return defaultPaymentTerms;
    }

    public void setDefaultPaymentTerms(Integer defaultPaymentTerms) {
        this.defaultPaymentTerms = defaultPaymentTerms;
    }
}
