package com.rishav.billing.auth.controller;

import com.rishav.billing.auth.dto.BusinessDto;
import com.rishav.billing.auth.dto.InviteMemberRequest;
import com.rishav.billing.auth.dto.MembershipDto;
import com.rishav.billing.auth.dto.UpdateBusinessRequest;
import com.rishav.billing.auth.service.BusinessService;
import com.rishav.billing.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/business")
public class BusinessController {

    private final BusinessService businessService;

    public BusinessController(BusinessService businessService) {
        this.businessService = businessService;
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<BusinessDto>> getProfile() {
        BusinessDto dto = businessService.getProfile();
        return ResponseEntity.ok(ApiResponse.ok(dto));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<BusinessDto>> updateProfile(@Valid @RequestBody UpdateBusinessRequest request) {
        BusinessDto dto = businessService.updateProfile(request);
        return ResponseEntity.ok(ApiResponse.ok(dto, "Business profile updated successfully"));
    }

    @PostMapping("/members")
    @PreAuthorize("hasRole('OWNER')")
    public ResponseEntity<ApiResponse<MembershipDto>> inviteMember(@Valid @RequestBody InviteMemberRequest request) {
        MembershipDto dto = businessService.inviteMember(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(dto, "Member invited successfully"));
    }

    @GetMapping("/members")
    public ResponseEntity<ApiResponse<List<MembershipDto>>> getMembers() {
        List<MembershipDto> members = businessService.getMembers();
        return ResponseEntity.ok(ApiResponse.ok(members));
    }
}
