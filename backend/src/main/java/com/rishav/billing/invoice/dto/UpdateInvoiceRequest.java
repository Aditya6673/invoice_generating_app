package com.rishav.billing.invoice.dto;

import jakarta.validation.Valid;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record UpdateInvoiceRequest(
    UUID customerId,
    LocalDate dueDate,
    String notes,
    @Valid List<InvoiceLineRequest> lines
) {}
