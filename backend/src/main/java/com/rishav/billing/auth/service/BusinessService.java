package com.rishav.billing.auth.service;

import com.rishav.billing.auth.dto.BusinessDto;
import com.rishav.billing.auth.dto.InviteMemberRequest;
import com.rishav.billing.auth.dto.MembershipDto;
import com.rishav.billing.auth.dto.UpdateBusinessRequest;
import com.rishav.billing.auth.entity.AppUser;
import com.rishav.billing.auth.entity.Business;
import com.rishav.billing.auth.entity.Membership;
import com.rishav.billing.auth.repository.AppUserRepository;
import com.rishav.billing.auth.repository.BusinessRepository;
import com.rishav.billing.auth.repository.MembershipRepository;
import com.rishav.billing.auth.validation.GstinValidator;
import com.rishav.billing.common.audit.AuditService;
import com.rishav.billing.common.exception.BusinessRuleException;
import com.rishav.billing.common.exception.ResourceNotFoundException;
import com.rishav.billing.common.security.SecurityContextHelper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class BusinessService {

    private final BusinessRepository businessRepository;
    private final AppUserRepository appUserRepository;
    private final MembershipRepository membershipRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    public BusinessService(
            BusinessRepository businessRepository,
            AppUserRepository appUserRepository,
            MembershipRepository membershipRepository,
            PasswordEncoder passwordEncoder,
            AuditService auditService
    ) {
        this.businessRepository = businessRepository;
        this.appUserRepository = appUserRepository;
        this.membershipRepository = membershipRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public BusinessDto getProfile() {
        UUID businessId = SecurityContextHelper.getCurrentBusinessId();
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business", "id", businessId));

        return BusinessDto.fromEntity(business);
    }

    @Transactional
    public BusinessDto updateProfile(UpdateBusinessRequest request) {
        UUID businessId = SecurityContextHelper.getCurrentBusinessId();
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business", "id", businessId));

        if (request.gstin() != null && !request.gstin().isBlank()) {
            if (!GstinValidator.isValid(request.gstin())) {
                throw new BusinessRuleException("INVALID_GSTIN", "Invalid GSTIN format or checksum");
            }
            business.setGstin(request.gstin().trim().toUpperCase());
        }

        if (request.name() != null && !request.name().isBlank()) {
            business.setName(request.name().trim());
        }
        if (request.address() != null) {
            business.setAddress(request.address().trim());
        }
        if (request.stateCode() != null) {
            business.setStateCode(request.stateCode().trim());
        }
        if (request.logoUrl() != null) {
            business.setLogoUrl(request.logoUrl().trim());
        }
        if (request.invoicePrefix() != null && !request.invoicePrefix().isBlank()) {
            business.setInvoicePrefix(request.invoicePrefix().trim());
        }
        if (request.bankName() != null) {
            business.setBankName(request.bankName().trim());
        }
        if (request.bankAccount() != null) {
            business.setBankAccount(request.bankAccount().trim());
        }
        if (request.bankIfsc() != null) {
            business.setBankIfsc(request.bankIfsc().trim());
        }
        if (request.defaultPaymentTerms() != null) {
            business.setDefaultPaymentTerms(request.defaultPaymentTerms());
        }

        business = businessRepository.save(business);
        auditService.log("BUSINESS", business.getId(), "UPDATE", "Business profile updated");

        return BusinessDto.fromEntity(business);
    }

    @Transactional
    public MembershipDto inviteMember(InviteMemberRequest request) {
        String currentRole = SecurityContextHelper.getCurrentRole();
        if (!"OWNER".equals(currentRole)) {
            throw new BusinessRuleException("ACCESS_DENIED", "Only an OWNER can invite members");
        }

        UUID businessId = SecurityContextHelper.getCurrentBusinessId();
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business", "id", businessId));

        String normalizedEmail = request.email().trim().toLowerCase();
        Optional<AppUser> existingUserOpt = appUserRepository.findByEmail(normalizedEmail);

        AppUser user;
        if (existingUserOpt.isPresent()) {
            user = existingUserOpt.get();
            if (membershipRepository.findByUserAndBusiness(user, business).isPresent()) {
                throw new BusinessRuleException("ALREADY_MEMBER", "User is already a member of this business");
            }
        } else {
            String tempPassword = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
            user = new AppUser();
            user.setEmail(normalizedEmail);
            user.setName(request.name().trim());
            user.setPasswordHash(passwordEncoder.encode(tempPassword));
            user.setActive(true);
            user = appUserRepository.save(user);
        }

        Membership membership = new Membership();
        membership.setUser(user);
        membership.setBusiness(business);
        membership.setRole(request.role());
        membership = membershipRepository.save(membership);

        auditService.log("MEMBERSHIP", membership.getId(), "INVITE", "Invited user " + normalizedEmail + " with role " + request.role());

        return MembershipDto.fromEntity(membership);
    }

    @Transactional(readOnly = true)
    public List<MembershipDto> getMembers() {
        UUID businessId = SecurityContextHelper.getCurrentBusinessId();
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business", "id", businessId));

        List<Membership> memberships = membershipRepository.findByBusiness(business);
        return memberships.stream()
                .map(MembershipDto::fromEntity)
                .toList();
    }
}
