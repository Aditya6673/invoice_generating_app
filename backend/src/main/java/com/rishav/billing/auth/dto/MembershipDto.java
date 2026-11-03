package com.rishav.billing.auth.dto;

import com.rishav.billing.auth.entity.Membership;

import java.util.UUID;

public record MembershipDto(
    UUID id,
    UUID userId,
    String userName,
    String userEmail,
    String role
) {
    public static MembershipDto fromEntity(Membership membership) {
        if (membership == null) {
            return null;
        }
        return new MembershipDto(
            membership.getId(),
            membership.getUser() != null ? membership.getUser().getId() : null,
            membership.getUser() != null ? membership.getUser().getName() : null,
            membership.getUser() != null ? membership.getUser().getEmail() : null,
            membership.getRole() != null ? membership.getRole().name() : null
        );
    }
}
