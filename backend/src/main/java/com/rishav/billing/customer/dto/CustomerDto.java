package com.rishav.billing.customer.dto;

import com.rishav.billing.customer.entity.Customer;

import java.time.Instant;
import java.util.UUID;

public record CustomerDto(
    UUID id,
    String name,
    String phone,
    String email,
    String billingAddress,
    String city,
    String state,
    String stateCode,
    String pincode,
    String gstin,
    Instant createdAt
) {
    public static CustomerDto fromEntity(Customer customer) {
        if (customer == null) {
            return null;
        }
        return new CustomerDto(
            customer.getId(),
            customer.getName(),
            customer.getPhone(),
            customer.getEmail(),
            customer.getBillingAddress(),
            customer.getCity(),
            customer.getState(),
            customer.getStateCode(),
            customer.getPincode(),
            customer.getGstin(),
            customer.getCreatedAt()
        );
    }
}
