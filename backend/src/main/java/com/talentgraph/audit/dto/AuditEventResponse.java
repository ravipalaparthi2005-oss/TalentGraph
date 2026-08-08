package com.talentgraph.audit.dto;

import com.talentgraph.audit.AuditEvent;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditEventResponse {

    private UUID id;
    private UUID organizationId;
    private UUID actorUserId;
    private String actorName;
    private String entityType;
    private UUID entityId;
    private String action;
    private String metadataJson;
    private Instant createdAt;

    public static AuditEventResponse fromEntity(AuditEvent event) {
        return AuditEventResponse.builder()
                .id(event.getId())
                .organizationId(event.getOrganization().getId())
                .actorUserId(event.getActorUser() != null ? event.getActorUser().getId() : null)
                .actorName(event.getActorUser() != null ? (event.getActorUser().getFirstName() + " " + event.getActorUser().getLastName()) : "System")
                .entityType(event.getEntityType())
                .entityId(event.getEntityId())
                .action(event.getAction())
                .metadataJson(event.getMetadataJson())
                .createdAt(event.getCreatedAt())
                .build();
    }
}
