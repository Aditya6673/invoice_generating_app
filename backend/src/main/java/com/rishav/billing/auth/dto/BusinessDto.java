package com.rishav.billing.auth.dto;

import com.rishav.billing.auth.entity.Business;

import java.time.Instant;
import java.util.UUID;

public record BusinessDto(
    UUID id,
    String name,
    String address,
    String gstin,
    String stateCode,
    String logoUrl,
    String invoicePrefix,
    String bankName,
    String bankAccount,
    String bankIfsc,
    Integer defaultPaymentTerms,
    Instant createdAt,
    Instant updatedAt
) {
    public static BusinessDto fromEntity(Business business) {
        if (business == null) {
            return null;
        }
        return new BusinessDto(
            business.getId(),
            business.getName(),
            business.getAddress(),
            business.getGstin(),
            business.getStateCode(),
            business.getLogoUrl(),
            business.getInvoicePrefix(),
            business.getBankName(),
            business.getBankAccount(),
            business.getBankIfsc(),
            business.getDefaultPaymentTerms(),
            business.getCreatedAt(),
            business.getUpdatedAt()
        );
    }
}
