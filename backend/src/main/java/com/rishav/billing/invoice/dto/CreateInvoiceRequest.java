package com.rishav.billing.invoice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record CreateInvoiceRequest(
    @NotNull UUID customerId,
    LocalDate dueDate,
    String currency,
    String notes,
    @NotEmpty @Valid List<InvoiceLineRequest> lines
) {
    public CreateInvoiceRequest {
        if (currency == null || currency.isBlank()) {
            currency = "INR";
        }
    }
}
