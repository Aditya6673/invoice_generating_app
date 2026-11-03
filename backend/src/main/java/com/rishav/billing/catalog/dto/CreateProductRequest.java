package com.rishav.billing.catalog.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateProductRequest(
    @NotBlank(message = "Product name is required")
    String name,

    @Size(max = 8, message = "HSN/SAC code must not exceed 8 characters")
    String hsnSacCode,

    @NotNull(message = "Default price is required")
    @DecimalMin(value = "0.00", message = "Default price must be greater than or equal to 0")
    BigDecimal defaultPrice,

    @NotNull(message = "Default tax rate is required")
    @DecimalMin(value = "0.00", message = "Default tax rate must be greater than or equal to 0")
    @DecimalMax(value = "100.00", message = "Default tax rate cannot exceed 100")
    BigDecimal defaultTaxRate,

    @NotBlank(message = "Unit is required")
    @Size(max = 10, message = "Unit must not exceed 10 characters")
    String unit
) {}
