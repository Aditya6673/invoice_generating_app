package com.rishav.billing.catalog.dto;

import com.rishav.billing.catalog.entity.Product;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductDto(
    UUID id,
    String name,
    String hsnSacCode,
    BigDecimal defaultPrice,
    BigDecimal defaultTaxRate,
    String unit,
    boolean active,
    Instant createdAt
) {
    public static ProductDto fromEntity(Product product) {
        if (product == null) {
            return null;
        }
        return new ProductDto(
            product.getId(),
            product.getName(),
            product.getHsnSacCode(),
            product.getDefaultPrice(),
            product.getDefaultTaxRate(),
            product.getUnit(),
            product.isActive(),
            product.getCreatedAt()
        );
    }
}
