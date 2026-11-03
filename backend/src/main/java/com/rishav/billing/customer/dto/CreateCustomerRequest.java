package com.rishav.billing.customer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCustomerRequest(
    @NotBlank(message = "Customer name is required")
    String name,

    @Size(max = 15, message = "Phone number must not exceed 15 characters")
    String phone,

    @Email(message = "Email must be a valid email address")
    String email,

    String billingAddress,

    String city,

    String state,

    @Size(max = 2, message = "State code must not exceed 2 characters")
    String stateCode,

    @Size(max = 6, message = "Pincode must not exceed 6 characters")
    String pincode,

    @Size(max = 15, message = "GSTIN must not exceed 15 characters")
    String gstin
) {}
