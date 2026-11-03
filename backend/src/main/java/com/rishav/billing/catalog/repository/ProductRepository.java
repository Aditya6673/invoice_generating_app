package com.rishav.billing.catalog.repository;

import com.rishav.billing.catalog.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    Page<Product> findByBusinessIdAndActiveTrue(UUID businessId, Pageable pageable);

    Page<Product> findByBusinessIdAndActiveTrueAndNameContainingIgnoreCase(UUID businessId, String name, Pageable pageable);

    Optional<Product> findByIdAndBusinessId(UUID id, UUID businessId);

    Page<Product> findByBusinessId(UUID businessId, Pageable pageable);
}
