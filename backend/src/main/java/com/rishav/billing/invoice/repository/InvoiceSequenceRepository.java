package com.rishav.billing.invoice.repository;

import com.rishav.billing.invoice.entity.InvoiceSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceSequenceRepository extends JpaRepository<InvoiceSequence, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM InvoiceSequence s WHERE s.businessId = :businessId AND s.financialYear = :fy")
    Optional<InvoiceSequence> findForUpdate(@Param("businessId") UUID businessId, @Param("fy") String fy);
}
