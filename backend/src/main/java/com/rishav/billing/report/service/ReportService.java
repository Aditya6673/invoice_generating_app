package com.rishav.billing.report.service;

import com.rishav.billing.common.security.SecurityContextHelper;
import com.rishav.billing.report.dto.InvoiceExportRow;
import com.rishav.billing.report.dto.MonthlySalesEntry;
import com.rishav.billing.report.dto.SummaryReport;
import com.rishav.billing.report.dto.TopCustomerEntry;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ReportService {

    private final EntityManager entityManager;

    public ReportService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public SummaryReport getSummary(LocalDate from, LocalDate to) {
        UUID businessId = SecurityContextHelper.getCurrentBusinessId();

        String sql = """
            SELECT
                COUNT(*) as invoice_count,
                COALESCE(SUM(grand_total), 0) as total_sales,
                COALESCE(SUM(grand_total - amount_paid), 0) as pending_amount,
                COALESCE(SUM(CASE WHEN status = 'OVERDUE' THEN grand_total - amount_paid ELSE 0 END), 0) as overdue_amount,
                COALESCE(SUM(amount_paid), 0) as total_collected
            FROM invoice
            WHERE business_id = :businessId
              AND status NOT IN ('DRAFT', 'VOID')
              AND (CAST(:from AS DATE) IS NULL OR issue_date >= :from)
              AND (CAST(:to AS DATE) IS NULL OR issue_date <= :to)
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("businessId", businessId);
        query.setParameter("from", from);
        query.setParameter("to", to);

        Object[] row = (Object[]) query.getSingleResult();
        return new SummaryReport(
                ((Number) row[0]).longValue(),
                toBigDecimal(row[1]),
                toBigDecimal(row[2]),
                toBigDecimal(row[3]),
                toBigDecimal(row[4])
        );
    }

    public List<MonthlySalesEntry> getMonthlySales(int year) {
        UUID businessId = SecurityContextHelper.getCurrentBusinessId();

        String sql = """
            SELECT
                TO_CHAR(date_trunc('month', issue_date), 'YYYY-MM') as month,
                COUNT(*) as count,
                COALESCE(SUM(grand_total), 0) as total
            FROM invoice
            WHERE business_id = :businessId
              AND status NOT IN ('DRAFT', 'VOID')
              AND EXTRACT(YEAR FROM issue_date) = :year
            GROUP BY date_trunc('month', issue_date)
            ORDER BY date_trunc('month', issue_date)
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("businessId", businessId);
        query.setParameter("year", year);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        List<MonthlySalesEntry> entries = new ArrayList<>();
        for (Object[] row : results) {
            entries.add(new MonthlySalesEntry(
                    (String) row[0],
                    ((Number) row[1]).longValue(),
                    toBigDecimal(row[2])
            ));
        }
        return entries;
    }

    public List<TopCustomerEntry> getTopCustomers(LocalDate from, LocalDate to, int limit) {
        UUID businessId = SecurityContextHelper.getCurrentBusinessId();

        String sql = """
            SELECT
                customer_name,
                COUNT(*) as invoice_count,
                COALESCE(SUM(grand_total), 0) as total_revenue
            FROM invoice
            WHERE business_id = :businessId
              AND status NOT IN ('DRAFT', 'VOID')
              AND (CAST(:from AS DATE) IS NULL OR issue_date >= :from)
              AND (CAST(:to AS DATE) IS NULL OR issue_date <= :to)
            GROUP BY customer_name
            ORDER BY total_revenue DESC
            LIMIT :limit
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("businessId", businessId);
        query.setParameter("from", from);
        query.setParameter("to", to);
        query.setParameter("limit", limit);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        List<TopCustomerEntry> entries = new ArrayList<>();
        for (Object[] row : results) {
            entries.add(new TopCustomerEntry(
                    (String) row[0],
                    ((Number) row[1]).longValue(),
                    toBigDecimal(row[2])
            ));
        }
        return entries;
    }

    public List<InvoiceExportRow> getExportData(LocalDate from, LocalDate to) {
        UUID businessId = SecurityContextHelper.getCurrentBusinessId();

        String sql = """
            SELECT
                invoice_number, customer_name, issue_date, due_date, status,
                subtotal, tax_total, grand_total, amount_paid,
                (grand_total - amount_paid) as balance
            FROM invoice
            WHERE business_id = :businessId
              AND status NOT IN ('DRAFT')
              AND (CAST(:from AS DATE) IS NULL OR issue_date >= :from)
              AND (CAST(:to AS DATE) IS NULL OR issue_date <= :to)
            ORDER BY issue_date DESC, invoice_number DESC
            """;

        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("businessId", businessId);
        query.setParameter("from", from);
        query.setParameter("to", to);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();

        List<InvoiceExportRow> rows = new ArrayList<>();
        for (Object[] row : results) {
            rows.add(new InvoiceExportRow(
                    (String) row[0],
                    (String) row[1],
                    row[2] != null ? ((java.sql.Date) row[2]).toLocalDate() : null,
                    row[3] != null ? ((java.sql.Date) row[3]).toLocalDate() : null,
                    (String) row[4],
                    toBigDecimal(row[5]),
                    toBigDecimal(row[6]),
                    toBigDecimal(row[7]),
                    toBigDecimal(row[8]),
                    toBigDecimal(row[9])
            ));
        }
        return rows;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal bd) return bd;
        return new BigDecimal(value.toString());
    }
}
