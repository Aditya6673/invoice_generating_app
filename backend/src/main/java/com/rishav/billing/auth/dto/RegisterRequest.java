package com.rishav.billing.auth.dto;

import com.rishav.billing.auth.validation.ValidGstin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    String email,

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    String password,

    @NotBlank(message = "Name is required")
    String name,

    @NotBlank(message = "Business name is required")
    String businessName,

    @ValidGstin
    String gstin,

    String stateCode,

    String phone
) {
}
