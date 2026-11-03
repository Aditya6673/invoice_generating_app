package com.rishav.billing.invoice.tax;

import java.math.BigDecimal;

public record TaxableLineItem(
        String description,
        String hsnSacCode,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal discountAmount,
        BigDecimal taxRate
) {
}
