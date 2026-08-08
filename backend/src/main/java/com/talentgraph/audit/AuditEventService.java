package com.talentgraph.audit;

import com.talentgraph.audit.dto.AuditEventResponse;
import com.talentgraph.auth.User;
import com.talentgraph.organization.Organization;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AuditEventService {

    private final AuditEventRepository auditEventRepository;

    public AuditEventService(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }

    @Transactional
    public AuditEvent logEvent(Organization organization, User actor, String entityType, UUID entityId, String action, String metadataJson) {
        AuditEvent event = AuditEvent.builder()
                .organization(organization)
                .actorUser(actor)
                .entityType(entityType)
                .entityId(entityId)
                .action(action)
                .metadataJson(metadataJson)
                .build();
        return auditEventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public List<AuditEventResponse> getEventsForEntity(UUID organizationId, UUID entityId) {
        List<AuditEvent> events = auditEventRepository.findByOrganizationIdAndEntityIdOrderByCreatedAtDesc(organizationId, entityId);
        return events.stream()
                .map(AuditEventResponse::fromEntity)
                .toList();
    }
}
