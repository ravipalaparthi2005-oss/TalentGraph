package com.talentgraph.organization;

import com.talentgraph.auth.User;
import com.talentgraph.auth.dto.UserOrganizationMembershipResponse;
import com.talentgraph.auth.service.CurrentUserService;
import com.talentgraph.common.ApiResponse;
import com.talentgraph.organization.service.OrganizationAuthorizationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/organizations")
public class OrganizationController {

    private final CurrentUserService currentUserService;
    private final OrganizationAuthorizationService authorizationService;
    private final OrganizationMemberRepository organizationMemberRepository;

    public OrganizationController(
            CurrentUserService currentUserService,
            OrganizationAuthorizationService authorizationService,
            OrganizationMemberRepository organizationMemberRepository
    ) {
        this.currentUserService = currentUserService;
        this.authorizationService = authorizationService;
        this.organizationMemberRepository = organizationMemberRepository;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserOrganizationMembershipResponse>>> getMyOrganizations() {
        User currentUser = currentUserService.getCurrentUser();
        List<OrganizationMember> memberships = organizationMemberRepository.findByUserId(currentUser.getId());

        List<UserOrganizationMembershipResponse> responses = memberships.stream()
                .map(m -> UserOrganizationMembershipResponse.builder()
                        .organizationId(m.getOrganization().getId())
                        .organizationName(m.getOrganization().getName())
                        .organizationSlug(m.getOrganization().getSlug())
                        .role(m.getRole())
                        .build())
                .toList();

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/{organizationId}")
    public ResponseEntity<ApiResponse<UserOrganizationMembershipResponse>> getOrganizationById(@PathVariable UUID organizationId) {
        User currentUser = currentUserService.getCurrentUser();
        OrganizationMember member = authorizationService.requireOrganizationMember(currentUser, organizationId);

        UserOrganizationMembershipResponse response = UserOrganizationMembershipResponse.builder()
                .organizationId(member.getOrganization().getId())
                .organizationName(member.getOrganization().getName())
                .organizationSlug(member.getOrganization().getSlug())
                .role(member.getRole())
                .build();

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
