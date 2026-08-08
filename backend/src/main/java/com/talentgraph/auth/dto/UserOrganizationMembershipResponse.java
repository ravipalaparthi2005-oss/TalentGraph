package com.talentgraph.auth.dto;

import com.talentgraph.organization.OrganizationRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserOrganizationMembershipResponse {
    private UUID organizationId;
    private String organizationName;
    private String organizationSlug;
    private OrganizationRole role;
}
