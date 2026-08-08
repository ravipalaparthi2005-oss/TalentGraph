package com.talentgraph.evidence;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "evidence_skills")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvidenceSkill {

    @EmbeddedId
    private EvidenceSkillId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("evidenceId")
    @JoinColumn(name = "evidence_id", nullable = false)
    private Evidence evidence;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("skillId")
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @Enumerated(EnumType.STRING)
    @Column(name = "relationship_type", nullable = false)
    private EvidenceRelationshipType relationshipType;
}
