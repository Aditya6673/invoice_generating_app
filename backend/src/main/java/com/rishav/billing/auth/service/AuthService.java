package com.rishav.billing.auth.service;

import com.rishav.billing.auth.dto.AuthResponse;
import com.rishav.billing.auth.dto.LoginRequest;
import com.rishav.billing.auth.dto.RegisterRequest;
import com.rishav.billing.auth.entity.AppUser;
import com.rishav.billing.auth.entity.Business;
import com.rishav.billing.auth.entity.Membership;
import com.rishav.billing.auth.entity.Role;
import com.rishav.billing.auth.repository.AppUserRepository;
import com.rishav.billing.auth.repository.BusinessRepository;
import com.rishav.billing.auth.repository.MembershipRepository;
import com.rishav.billing.auth.security.JwtService;
import com.rishav.billing.auth.validation.GstinValidator;
import com.rishav.billing.common.audit.AuditService;
import com.rishav.billing.common.exception.BusinessRuleException;
import com.rishav.billing.common.exception.ResourceNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final BusinessRepository businessRepository;
    private final MembershipRepository membershipRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;

    public AuthService(
            AppUserRepository appUserRepository,
            BusinessRepository businessRepository,
            MembershipRepository membershipRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuditService auditService
    ) {
        this.appUserRepository = appUserRepository;
        this.businessRepository = businessRepository;
        this.membershipRepository = membershipRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditService = auditService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        if (appUserRepository.existsByEmail(normalizedEmail)) {
            throw new BusinessRuleException("EMAIL_ALREADY_EXISTS", "Email is already registered: " + normalizedEmail);
        }

        if (request.gstin() != null && !request.gstin().isBlank()) {
            if (!GstinValidator.isValid(request.gstin())) {
                throw new BusinessRuleException("INVALID_GSTIN", "Invalid GSTIN format or checksum");
            }
        }

        Business business = new Business();
        business.setName(request.businessName().trim());
        if (request.gstin() != null && !request.gstin().isBlank()) {
            business.setGstin(request.gstin().trim().toUpperCase());
        }
        if (request.stateCode() != null && !request.stateCode().isBlank()) {
            business.setStateCode(request.stateCode().trim());
        }
        business.setInvoicePrefix("INV");
        business.setDefaultPaymentTerms(30);
        business = businessRepository.save(business);

        AppUser user = new AppUser();
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setName(request.name().trim());
        if (request.phone() != null && !request.phone().isBlank()) {
            user.setPhone(request.phone().trim());
        }
        user.setActive(true);
        user = appUserRepository.save(user);

        Membership membership = new Membership();
        membership.setUser(user);
        membership.setBusiness(business);
        membership.setRole(Role.OWNER);
        membership = membershipRepository.save(membership);

        String accessToken = jwtService.generateAccessToken(user, membership);
        String refreshToken = jwtService.generateRefreshToken(user);

        auditService.log("USER", user.getId(), "REGISTER", "User registered as OWNER of business " + business.getName());

        AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
                user.getId(),
                user.getEmail(),
                user.getName(),
                membership.getRole().name(),
                business.getId()
        );

        return new AuthResponse(accessToken, refreshToken, userInfo);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();

        AppUser user = appUserRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        if (!user.isActive()) {
            throw new BusinessRuleException("USER_INACTIVE", "User account has been deactivated");
        }

        List<Membership> memberships = membershipRepository.findByUser(user);
        if (memberships.isEmpty()) {
            throw new BusinessRuleException("NO_MEMBERSHIP", "User is not associated with any business");
        }

        Membership membership = memberships.get(0);

        String accessToken = jwtService.generateAccessToken(user, membership);
        String refreshToken = jwtService.generateRefreshToken(user);

        auditService.log("USER", user.getId(), "LOGIN", "User logged in successfully");

        AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
                user.getId(),
                user.getEmail(),
                user.getName(),
                membership.getRole().name(),
                membership.getBusiness().getId()
        );

        return new AuthResponse(accessToken, refreshToken, userInfo);
    }

    @Transactional(readOnly = true)
    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtService.isTokenValid(refreshToken)) {
            throw new BusinessRuleException("INVALID_TOKEN", "Refresh token is invalid or expired");
        }

        String email = jwtService.extractEmail(refreshToken);
        if (email == null) {
            throw new BusinessRuleException("INVALID_TOKEN", "Unable to extract email from refresh token");
        }

        AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("AppUser", "email", email));

        if (!user.isActive()) {
            throw new BusinessRuleException("USER_INACTIVE", "User account has been deactivated");
        }

        List<Membership> memberships = membershipRepository.findByUser(user);
        if (memberships.isEmpty()) {
            throw new BusinessRuleException("NO_MEMBERSHIP", "User is not associated with any business");
        }

        Membership membership = memberships.get(0);
        String newAccessToken = jwtService.generateAccessToken(user, membership);

        AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
                user.getId(),
                user.getEmail(),
                user.getName(),
                membership.getRole().name(),
                membership.getBusiness().getId()
        );

        return new AuthResponse(newAccessToken, refreshToken, userInfo);
    }
}
