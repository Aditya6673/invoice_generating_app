package com.rishav.billing.invoice.tax;

import java.math.BigDecimal;
import java.util.List;

public record TaxResult(
        boolean intraState,
        List<TaxLineResult> lineResults,
        BigDecimal subtotal,
        BigDecimal discountTotal,
        BigDecimal taxTotal,
        BigDecimal grandTotal
) {
}
