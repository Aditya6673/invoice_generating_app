package com.rishav.billing.invoice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.UUID;

@Entity
@Table(
    name = "invoice_sequence",
    uniqueConstraints = @UniqueConstraint(name = "uk_invoice_sequence_business_fy", columnNames = {"business_id", "financial_year"})
)
public class InvoiceSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(name = "financial_year", length = 7, nullable = false)
    private String financialYear;

    @Column(name = "last_number", nullable = false)
    private int lastNumber = 0;

    public InvoiceSequence() {
    }

    public InvoiceSequence(UUID businessId, String financialYear, int lastNumber) {
        this.businessId = businessId;
        this.financialYear = financialYear;
        this.lastNumber = lastNumber;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getBusinessId() {
        return businessId;
    }

    public void setBusinessId(UUID businessId) {
        this.businessId = businessId;
    }

    public String getFinancialYear() {
        return financialYear;
    }

    public void setFinancialYear(String financialYear) {
        this.financialYear = financialYear;
    }

    public int getLastNumber() {
        return lastNumber;
    }

    public void setLastNumber(int lastNumber) {
        this.lastNumber = lastNumber;
    }
}
