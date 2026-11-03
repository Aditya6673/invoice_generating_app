package com.rishav.billing.catalog.entity;

import com.rishav.billing.common.entity.TenantEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;

import java.math.BigDecimal;

@Entity
@Table(name = "product")
@Filter(name = "tenantFilter")
public class Product extends TenantEntity {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "hsn_sac_code", length = 8)
    private String hsnSacCode;

    @Column(name = "default_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal defaultPrice = BigDecimal.ZERO;

    @Column(name = "default_tax_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal defaultTaxRate = BigDecimal.ZERO;

    @Column(name = "unit", nullable = false, length = 10)
    private String unit = "PCS";

    @Column(name = "active", nullable = false)
    private boolean active = true;

    public Product() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHsnSacCode() {
        return hsnSacCode;
    }

    public void setHsnSacCode(String hsnSacCode) {
        this.hsnSacCode = hsnSacCode;
    }

    public BigDecimal getDefaultPrice() {
        return defaultPrice;
    }

    public void setDefaultPrice(BigDecimal defaultPrice) {
        this.defaultPrice = defaultPrice;
    }

    public BigDecimal getDefaultTaxRate() {
        return defaultTaxRate;
    }

    public void setDefaultTaxRate(BigDecimal defaultTaxRate) {
        this.defaultTaxRate = defaultTaxRate;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
