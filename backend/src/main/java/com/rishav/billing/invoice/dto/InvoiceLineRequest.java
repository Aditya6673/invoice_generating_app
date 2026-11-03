package com.rishav.billing.invoice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record InvoiceLineRequest(
    @NotBlank String description,
    String hsnSacCode,
    @NotNull @DecimalMin("0.01") BigDecimal quantity,
    @NotNull @DecimalMin("0") BigDecimal unitPrice,
    BigDecimal discountAmount,
    @NotNull @DecimalMin("0") BigDecimal taxRate
) {
    public InvoiceLineRequest {
        if (discountAmount == null) {
            discountAmount = BigDecimal.ZERO;
        }
    }
}
