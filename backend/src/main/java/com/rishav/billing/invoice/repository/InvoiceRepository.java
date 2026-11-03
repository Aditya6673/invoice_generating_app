package com.rishav.billing.invoice.repository;

import com.rishav.billing.invoice.entity.Invoice;
import com.rishav.billing.invoice.entity.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    Optional<Invoice> findByIdAndBusinessId(UUID id, UUID businessId);

    @Query("SELECT DISTINCT i FROM Invoice i LEFT JOIN FETCH i.lines WHERE i.id = :id AND i.businessId = :businessId")
    Optional<Invoice> findByIdAndBusinessIdWithLines(@Param("id") UUID id, @Param("businessId") UUID businessId);

    Page<Invoice> findByBusinessId(UUID businessId, Pageable pageable);

    Page<Invoice> findByBusinessIdAndStatus(UUID businessId, InvoiceStatus status, Pageable pageable);

    Page<Invoice> findByBusinessIdAndCustomerId(UUID businessId, UUID customerId, Pageable pageable);

    @Query("SELECT i FROM Invoice i WHERE i.businessId = :businessId " +
           "AND i.issueDate >= :startDate AND i.issueDate <= :endDate")
    Page<Invoice> findByBusinessIdAndIssueDateBetween(
            @Param("businessId") UUID businessId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            Pageable pageable
    );

    @Query("SELECT i FROM Invoice i WHERE i.businessId = :businessId " +
           "AND i.status NOT IN (com.rishav.billing.invoice.entity.InvoiceStatus.PAID, com.rishav.billing.invoice.entity.InvoiceStatus.VOID, com.rishav.billing.invoice.entity.InvoiceStatus.DRAFT) " +
           "AND i.dueDate < :today " +
           "AND i.amountPaid < i.grandTotal")
    List<Invoice> findOverdueInvoices(
            @Param("businessId") UUID businessId,
            @Param("today") LocalDate today
    );

    @Query("SELECT i FROM Invoice i WHERE i.businessId = :businessId " +
           "AND (:status IS NULL OR i.status = :status) " +
           "AND (:customerId IS NULL OR i.customerId = :customerId) " +
           "AND (CAST(:fromDate AS date) IS NULL OR i.issueDate >= :fromDate) " +
           "AND (CAST(:toDate AS date) IS NULL OR i.issueDate <= :toDate)")
    Page<Invoice> findWithFilters(
            @Param("businessId") UUID businessId,
            @Param("status") InvoiceStatus status,
            @Param("customerId") UUID customerId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            Pageable pageable
    );

    List<Invoice> findByStatusInAndDueDateBefore(java.util.Collection<InvoiceStatus> statuses, LocalDate dueDate);
}
