package com.rishav.billing.invoice.tax;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TaxCalculatorTest {

    private TaxCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new TaxCalculator();
    }

    private TaxableLineItem line(String desc, String hsn, double qty, double price, double discount, double taxRate) {
        return new TaxableLineItem(desc, hsn, bd(qty), bd(price), bd(discount), bd(taxRate));
    }

    private BigDecimal bd(double val) {
        return BigDecimal.valueOf(val).setScale(2, RoundingMode.HALF_UP);
    }

    @Nested
    @DisplayName("Intra-State (CGST + SGST)")
    class IntraStateTests {

        @Test
        @DisplayName("Single item, 18% GST, same state -> 9% CGST + 9% SGST")
        void singleItemSameState() {
            List<TaxableLineItem> items = List.of(
                    line("Laptop", "8471", 1, 50000, 0, 18)
            );

            TaxResult result = calculator.calculate("29", "29", items);

            assertTrue(result.intraState());
            assertEquals(1, result.lineResults().size());

            TaxLineResult lr = result.lineResults().get(0);
            assertEquals(bd(50000), lr.taxableAmount());
            assertEquals(bd(9), lr.cgstRate());
            assertEquals(bd(4500), lr.cgstAmount());
            assertEquals(bd(9), lr.sgstRate());
            assertEquals(bd(4500), lr.sgstAmount());
            assertEquals(bd(0), lr.igstRate());
            assertEquals(bd(0), lr.igstAmount());
            assertEquals(bd(59000), lr.lineTotal());

            assertEquals(bd(50000), result.subtotal());
            assertEquals(bd(0), result.discountTotal());
            assertEquals(bd(9000), result.taxTotal());
            assertEquals(bd(59000), result.grandTotal());
        }

        @Test
        @DisplayName("Item with discount, 18% GST, same state")
        void itemWithDiscount() {
            List<TaxableLineItem> items = List.of(
                    line("Service", "9983", 1, 10000, 1000, 18)
            );

            TaxResult result = calculator.calculate("27", "27", items);

            assertTrue(result.intraState());
            TaxLineResult lr = result.lineResults().get(0);
            // Taxable: 10000 - 1000 = 9000
            assertEquals(bd(9000), lr.taxableAmount());
            // CGST: 9000 * 9% = 810
            assertEquals(bd(810), lr.cgstAmount());
            assertEquals(bd(810), lr.sgstAmount());
            // Line total: 9000 + 810 + 810 = 10620
            assertEquals(bd(10620), lr.lineTotal());

            assertEquals(bd(10000), result.subtotal());
            assertEquals(bd(1000), result.discountTotal());
            assertEquals(bd(1620), result.taxTotal());
            assertEquals(bd(10620), result.grandTotal());
        }

        @Test
        @DisplayName("Multiple items, same state")
        void multipleItems() {
            List<TaxableLineItem> items = List.of(
                    line("Item A", "1001", 2, 500, 0, 12),
                    line("Item B", "1002", 3, 1000, 100, 18)
            );

            TaxResult result = calculator.calculate("06", "06", items);

            assertTrue(result.intraState());
            assertEquals(2, result.lineResults().size());

            // Item A: taxable = 2*500 = 1000, CGST = 1000*6% = 60, SGST = 60
            TaxLineResult lr0 = result.lineResults().get(0);
            assertEquals(bd(1000), lr0.taxableAmount());
            assertEquals(bd(60), lr0.cgstAmount());
            assertEquals(bd(60), lr0.sgstAmount());
            assertEquals(bd(1120), lr0.lineTotal());

            // Item B: taxable = 3*1000 - 100 = 2900, CGST = 2900*9% = 261, SGST = 261
            TaxLineResult lr1 = result.lineResults().get(1);
            assertEquals(bd(2900), lr1.taxableAmount());
            assertEquals(bd(261), lr1.cgstAmount());
            assertEquals(bd(261), lr1.sgstAmount());
            assertEquals(bd(3422), lr1.lineTotal());

            assertEquals(bd(4000), result.subtotal()); // 1000 + 3000
            assertEquals(bd(100), result.discountTotal());
            assertEquals(bd(642), result.taxTotal()); // 120 + 522
            assertEquals(bd(4542), result.grandTotal()); // 1120 + 3422
        }
    }

    @Nested
    @DisplayName("Inter-State (IGST)")
    class InterStateTests {

        @Test
        @DisplayName("Single item, 18% GST, different state -> 18% IGST")
        void singleItemDifferentState() {
            List<TaxableLineItem> items = List.of(
                    line("Laptop", "8471", 1, 50000, 0, 18)
            );

            TaxResult result = calculator.calculate("29", "27", items);

            assertFalse(result.intraState());
            TaxLineResult lr = result.lineResults().get(0);
            assertEquals(bd(50000), lr.taxableAmount());
            assertEquals(bd(0), lr.cgstAmount());
            assertEquals(bd(0), lr.sgstAmount());
            assertEquals(bd(18), lr.igstRate());
            assertEquals(bd(9000), lr.igstAmount());
            assertEquals(bd(59000), lr.lineTotal());
        }

        @Test
        @DisplayName("Item with discount, different state")
        void itemWithDiscountInterState() {
            List<TaxableLineItem> items = List.of(
                    line("Consulting", "9983", 5, 2000, 500, 18)
            );

            TaxResult result = calculator.calculate("29", "06", items);

            assertFalse(result.intraState());
            TaxLineResult lr = result.lineResults().get(0);
            // Taxable: 5*2000 - 500 = 9500
            assertEquals(bd(9500), lr.taxableAmount());
            // IGST: 9500 * 18% = 1710
            assertEquals(bd(1710), lr.igstAmount());
            assertEquals(bd(11210), lr.lineTotal());
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Empty item list returns zero totals")
        void emptyItems() {
            TaxResult result = calculator.calculate("29", "29", List.of());
            assertEquals(bd(0), result.grandTotal());
            assertEquals(bd(0), result.taxTotal());
            assertTrue(result.lineResults().isEmpty());
        }

        @Test
        @DisplayName("Null item list returns zero totals")
        void nullItems() {
            TaxResult result = calculator.calculate("29", "29", null);
            assertEquals(bd(0), result.grandTotal());
        }

        @Test
        @DisplayName("Zero tax rate produces no tax")
        void zeroTaxRate() {
            List<TaxableLineItem> items = List.of(
                    line("Exempt Item", "0000", 1, 1000, 0, 0)
            );

            TaxResult result = calculator.calculate("29", "29", items);
            TaxLineResult lr = result.lineResults().get(0);
            assertEquals(bd(0), lr.cgstAmount());
            assertEquals(bd(0), lr.sgstAmount());
            assertEquals(bd(0), lr.igstAmount());
            assertEquals(bd(1000), lr.lineTotal());
            assertEquals(bd(0), result.taxTotal());
        }

        @Test
        @DisplayName("Discount exceeding gross amount clamps taxable to zero")
        void discountExceedsGross() {
            List<TaxableLineItem> items = List.of(
                    line("Free item", "0000", 1, 100, 200, 18)
            );

            TaxResult result = calculator.calculate("29", "29", items);
            TaxLineResult lr = result.lineResults().get(0);
            assertEquals(bd(0), lr.taxableAmount());
            assertEquals(bd(0), lr.lineTotal());
        }

        @Test
        @DisplayName("Null state codes treated as inter-state")
        void nullStateCodes() {
            List<TaxableLineItem> items = List.of(
                    line("Item", "1234", 1, 1000, 0, 18)
            );

            TaxResult result = calculator.calculate(null, "29", items);
            assertFalse(result.intraState());
            assertEquals(bd(180), result.lineResults().get(0).igstAmount());
        }

        @Test
        @DisplayName("Odd tax rate (5%) splits correctly for CGST/SGST")
        void oddTaxRate() {
            List<TaxableLineItem> items = List.of(
                    line("Food", "2106", 1, 1000, 0, 5)
            );

            TaxResult result = calculator.calculate("07", "07", items);
            assertTrue(result.intraState());
            TaxLineResult lr = result.lineResults().get(0);
            // 5% / 2 = 2.50 each
            assertEquals(bd(2.50), lr.cgstRate());
            assertEquals(bd(2.50), lr.sgstRate());
            // 1000 * 2.5% = 25
            assertEquals(bd(25), lr.cgstAmount());
            assertEquals(bd(25), lr.sgstAmount());
            assertEquals(bd(1050), lr.lineTotal());
        }

        @Test
        @DisplayName("Fractional quantity produces correct taxable amount")
        void fractionalQuantity() {
            List<TaxableLineItem> items = List.of(
                    line("Service Hours", "9983", 1.5, 2000, 0, 18)
            );

            TaxResult result = calculator.calculate("29", "06", items);
            TaxLineResult lr = result.lineResults().get(0);
            // 1.5 * 2000 = 3000
            assertEquals(bd(3000), lr.taxableAmount());
            // IGST: 3000 * 18% = 540
            assertEquals(bd(540), lr.igstAmount());
            assertEquals(bd(3540), lr.lineTotal());
        }
    }
}
