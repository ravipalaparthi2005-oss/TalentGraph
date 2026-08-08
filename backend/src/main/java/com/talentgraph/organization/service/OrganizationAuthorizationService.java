package com.talentgraph.organization.service;

import com.talentgraph.auth.User;
import com.talentgraph.common.exception.ForbiddenException;
import com.talentgraph.organization.OrganizationMember;
import com.talentgraph.organization.OrganizationMemberRepository;
import com.talentgraph.organization.OrganizationRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class OrganizationAuthorizationService {

    private final OrganizationMemberRepository organizationMemberRepository;

    private static final Map<OrganizationRole, Integer> ROLE_RANK = Map.of(
            OrganizationRole.OWNER, 50,
            OrganizationRole.ADMIN, 40,
            OrganizationRole.RECRUITER, 30,
            OrganizationRole.HIRING_MANAGER, 20,
            OrganizationRole.INTERVIEWER, 10
    );

    public OrganizationAuthorizationService(OrganizationMemberRepository organizationMemberRepository) {
        this.organizationMemberRepository = organizationMemberRepository;
    }

    public OrganizationMember requireOrganizationMember(User user, UUID organizationId) {
        if (user == null || organizationId == null) {
            throw new ForbiddenException("Access denied: Invalid user or organization context");
        }

        return organizationMemberRepository.findByOrganizationIdAndUserId(organizationId, user.getId())
                .orElseThrow(() -> new ForbiddenException("Access denied: User is not a member of this organization"));
    }

    public OrganizationMember requireRole(User user, UUID organizationId, OrganizationRole minimumRole) {
        OrganizationMember member = requireOrganizationMember(user, organizationId);
        int userRoleRank = ROLE_RANK.getOrDefault(member.getRole(), 0);
        int requiredRank = ROLE_RANK.getOrDefault(minimumRole, 0);

        if (userRoleRank < requiredRank) {
            throw new ForbiddenException("Access denied: Requires " + minimumRole + " role or higher");
        }

        return member;
    }

    public void verifyResourceAccess(User user, UUID resourceOrganizationId) {
        requireOrganizationMember(user, resourceOrganizationId);
    }
}
