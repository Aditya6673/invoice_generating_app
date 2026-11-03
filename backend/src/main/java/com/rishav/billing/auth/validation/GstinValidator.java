package com.rishav.billing.auth.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

public class GstinValidator implements ConstraintValidator<ValidGstin, String> {

    private static final String CODE_POINT_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int MODULUS = 36;
    private static final Pattern GSTIN_PATTERN = Pattern.compile("^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]$");

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        return isValid(value);
    }

    /**
     * Validates a GSTIN string:
     * 1. 15 characters
     * 2. First 2 digits (01-37 state code)
     * 3. Next 10 characters PAN (5 letters, 4 numbers, 1 letter)
     * 4. 13th character entity number (1-9 or A-Z)
     * 5. 14th character 'Z'
     * 6. 15th character checksum using Luhn mod 36 algorithm
     */
    public static boolean isValid(String gstin) {
        if (gstin == null) {
            return false;
        }

        String normalized = gstin.trim().toUpperCase();
        if (normalized.length() != 15) {
            return false;
        }

        if (!GSTIN_PATTERN.matcher(normalized).matches()) {
            return false;
        }

        try {
            int stateCode = Integer.parseInt(normalized.substring(0, 2));
            if (stateCode < 1 || stateCode > 37) {
                return false;
            }

            char expectedCheckDigit = normalized.charAt(14);
            char calculatedCheckDigit = calculateCheckDigit(normalized.substring(0, 14));
            return expectedCheckDigit == calculatedCheckDigit;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Calculates the 15th checksum character using the Luhn mod 36 algorithm.
     * Factors alternate between 1 (odd 1-based index) and 2 (even 1-based index)
     * from left to right.
     */
    public static char calculateCheckDigit(String gstin14) {
        if (gstin14 == null || gstin14.length() != 14) {
            throw new IllegalArgumentException("GSTIN payload for checksum must be exactly 14 characters");
        }

        int totalSum = 0;
        for (int i = 0; i < 14; i++) {
            char c = gstin14.charAt(i);
            int codePoint = CODE_POINT_CHARS.indexOf(c);
            if (codePoint < 0) {
                throw new IllegalArgumentException("Invalid character in GSTIN: " + c);
            }

            // 1-based index: odd -> 1, even -> 2 (0-based index: i%2==0 -> 1, i%2==1 -> 2)
            int factor = (i % 2 == 0) ? 1 : 2;
            int product = codePoint * factor;
            int digitSum = (product / MODULUS) + (product % MODULUS);
            totalSum += digitSum;
        }

        int remainder = totalSum % MODULUS;
        int checkIndex = (MODULUS - remainder) % MODULUS;
        return CODE_POINT_CHARS.charAt(checkIndex);
    }
}
