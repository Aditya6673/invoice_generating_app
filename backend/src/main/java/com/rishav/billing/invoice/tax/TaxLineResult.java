package com.rishav.billing.invoice.tax;

import java.math.BigDecimal;

public record TaxLineResult(
        BigDecimal taxableAmount,
        BigDecimal cgstRate,
        BigDecimal cgstAmount,
        BigDecimal sgstRate,
        BigDecimal sgstAmount,
        BigDecimal igstRate,
        BigDecimal igstAmount,
        BigDecimal lineTotal
) {
}
