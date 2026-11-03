package com.rishav.billing.auth.security;

import com.rishav.billing.auth.entity.AppUser;
import com.rishav.billing.auth.entity.Membership;
import com.rishav.billing.auth.repository.AppUserRepository;
import com.rishav.billing.auth.repository.MembershipRepository;
import com.rishav.billing.common.security.BillingUserDetails;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final AppUserRepository appUserRepository;
    private final MembershipRepository membershipRepository;

    public UserDetailsServiceImpl(AppUserRepository appUserRepository, MembershipRepository membershipRepository) {
        this.appUserRepository = appUserRepository;
        this.membershipRepository = membershipRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        List<Membership> memberships = membershipRepository.findByUser(user);
        Membership firstMembership = memberships.isEmpty() ? null : memberships.get(0);

        UUID businessId = (firstMembership != null && firstMembership.getBusiness() != null)
                ? firstMembership.getBusiness().getId()
                : null;
        String role = (firstMembership != null && firstMembership.getRole() != null)
                ? firstMembership.getRole().name()
                : "STAFF";

        return new BillingUserDetails(
                user.getId(),
                businessId,
                user.getEmail(),
                user.getPasswordHash(),
                role,
                user.isActive()
        );
    }
}
