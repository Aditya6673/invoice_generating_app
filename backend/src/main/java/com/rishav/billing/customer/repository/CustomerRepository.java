package com.rishav.billing.customer.repository;

import com.rishav.billing.customer.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Page<Customer> findByBusinessId(UUID businessId, Pageable pageable);

    Page<Customer> findByBusinessIdAndNameContainingIgnoreCase(UUID businessId, String name, Pageable pageable);

    Optional<Customer> findByIdAndBusinessId(UUID id, UUID businessId);

    @Query(value = "SELECT * FROM customer WHERE id = :id", nativeQuery = true)
    Optional<Customer> findByIdIncludingDeleted(@Param("id") UUID id);

    @Query(value = "SELECT * FROM customer WHERE id = :id AND business_id = :businessId", nativeQuery = true)
    Optional<Customer> findByIdIncludingDeleted(@Param("id") UUID id, @Param("businessId") UUID businessId);
}
