package com.rishav.billing.auth.dto;

import com.rishav.billing.auth.validation.ValidGstin;

public record UpdateBusinessRequest(
    String name,
    String address,
    @ValidGstin
    String gstin,
    String stateCode,
    String logoUrl,
    String invoicePrefix,
    String bankName,
    String bankAccount,
    String bankIfsc,
    Integer defaultPaymentTerms
) {
}
