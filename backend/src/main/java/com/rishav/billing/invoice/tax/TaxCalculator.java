package com.rishav.billing.invoice.tax;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure stateless component responsible for GST tax computations.
 * All monetary amounts are computed using BigDecimal with scale 2 and RoundingMode.HALF_UP.
 */
@Component
public class TaxCalculator {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final BigDecimal TWO = new BigDecimal("2");
    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    public TaxResult calculate(String businessStateCode, String customerStateCode, List<TaxableLineItem> items) {
        boolean intraState = businessStateCode != null
                && customerStateCode != null
                && businessStateCode.trim().equalsIgnoreCase(customerStateCode.trim());

        if (items == null || items.isEmpty()) {
            return new TaxResult(
                    intraState,
                    List.of(),
                    BigDecimal.ZERO.setScale(SCALE, ROUNDING),
                    BigDecimal.ZERO.setScale(SCALE, ROUNDING),
                    BigDecimal.ZERO.setScale(SCALE, ROUNDING),
                    BigDecimal.ZERO.setScale(SCALE, ROUNDING)
            );
        }

        List<TaxLineResult> lineResults = new ArrayList<>(items.size());
        BigDecimal subtotal = BigDecimal.ZERO.setScale(SCALE, ROUNDING);
        BigDecimal discountTotal = BigDecimal.ZERO.setScale(SCALE, ROUNDING);
        BigDecimal taxTotal = BigDecimal.ZERO.setScale(SCALE, ROUNDING);
        BigDecimal grandTotal = BigDecimal.ZERO.setScale(SCALE, ROUNDING);

        for (TaxableLineItem item : items) {
            BigDecimal quantity = item.quantity() != null ? item.quantity() : BigDecimal.ONE;
            BigDecimal unitPrice = item.unitPrice() != null ? item.unitPrice() : BigDecimal.ZERO;
            BigDecimal discountAmount = item.discountAmount() != null ? item.discountAmount() : BigDecimal.ZERO;
            BigDecimal taxRate = item.taxRate() != null ? item.taxRate() : BigDecimal.ZERO;

            // Gross line amount before discount
            BigDecimal grossLineAmount = quantity.multiply(unitPrice).setScale(SCALE, ROUNDING);
            discountAmount = discountAmount.setScale(SCALE, ROUNDING);

            // Taxable amount = (quantity * unitPrice) - discountAmount, rounded HALF_UP to 2 decimal places
            BigDecimal taxableAmount = grossLineAmount.subtract(discountAmount).setScale(SCALE, ROUNDING);
            if (taxableAmount.compareTo(BigDecimal.ZERO) < 0) {
                taxableAmount = BigDecimal.ZERO.setScale(SCALE, ROUNDING);
            }

            BigDecimal cgstRate;
            BigDecimal cgstAmount;
            BigDecimal sgstRate;
            BigDecimal sgstAmount;
            BigDecimal igstRate;
            BigDecimal igstAmount;

            if (intraState) {
                cgstRate = taxRate.divide(TWO, SCALE, ROUNDING);
                sgstRate = taxRate.divide(TWO, SCALE, ROUNDING);
                cgstAmount = taxableAmount.multiply(cgstRate).divide(ONE_HUNDRED, SCALE, ROUNDING);
                sgstAmount = taxableAmount.multiply(sgstRate).divide(ONE_HUNDRED, SCALE, ROUNDING);
                igstRate = BigDecimal.ZERO.setScale(SCALE, ROUNDING);
                igstAmount = BigDecimal.ZERO.setScale(SCALE, ROUNDING);
            } else {
                cgstRate = BigDecimal.ZERO.setScale(SCALE, ROUNDING);
                cgstAmount = BigDecimal.ZERO.setScale(SCALE, ROUNDING);
                sgstRate = BigDecimal.ZERO.setScale(SCALE, ROUNDING);
                sgstAmount = BigDecimal.ZERO.setScale(SCALE, ROUNDING);
                igstRate = taxRate.setScale(SCALE, ROUNDING);
                igstAmount = taxableAmount.multiply(igstRate).divide(ONE_HUNDRED, SCALE, ROUNDING);
            }

            BigDecimal lineTotal = taxableAmount
                    .add(cgstAmount)
                    .add(sgstAmount)
                    .add(igstAmount)
                    .setScale(SCALE, ROUNDING);

            lineResults.add(new TaxLineResult(
                    taxableAmount,
                    cgstRate,
                    cgstAmount,
                    sgstRate,
                    sgstAmount,
                    igstRate,
                    igstAmount,
                    lineTotal
            ));

            subtotal = subtotal.add(grossLineAmount);
            discountTotal = discountTotal.add(discountAmount);
            taxTotal = taxTotal.add(cgstAmount).add(sgstAmount).add(igstAmount);
            grandTotal = grandTotal.add(lineTotal);
        }

        return new TaxResult(
                intraState,
                lineResults,
                subtotal.setScale(SCALE, ROUNDING),
                discountTotal.setScale(SCALE, ROUNDING),
                taxTotal.setScale(SCALE, ROUNDING),
                grandTotal.setScale(SCALE, ROUNDING)
        );
    }
}
