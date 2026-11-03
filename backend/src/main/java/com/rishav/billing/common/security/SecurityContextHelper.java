package com.rishav.billing.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

/**
 * Utility to extract identity information from the current Spring Security context.
 * The JWT authentication populates a {@link BillingUserDetails} as the principal.
 */
public final class SecurityContextHelper {

    private SecurityContextHelper() {
    }

    public static UUID getCurrentUserId() {
        BillingUserDetails details = getRequiredUserDetails();
        return details.getUserId();
    }

    public static UUID getCurrentUserIdOrNull() {
        BillingUserDetails details = getUserDetailsOrNull();
        return details != null ? details.getUserId() : null;
    }

    public static UUID getCurrentBusinessId() {
        BillingUserDetails details = getRequiredUserDetails();
        return details.getBusinessId();
    }

    public static UUID getCurrentBusinessIdOrNull() {
        BillingUserDetails details = getUserDetailsOrNull();
        return details != null ? details.getBusinessId() : null;
    }

    public static String getCurrentRole() {
        BillingUserDetails details = getRequiredUserDetails();
        return details.getRole();
    }

    private static BillingUserDetails getRequiredUserDetails() {
        BillingUserDetails details = getUserDetailsOrNull();
        if (details == null) {
            throw new IllegalStateException("No authenticated user in security context");
        }
        return details;
    }

    private static BillingUserDetails getUserDetailsOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof BillingUserDetails details) {
            return details;
        }
        return null;
    }
}
