package com.talentgraph.evidence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvidenceSkillId implements Serializable {

    @Column(name = "evidence_id")
    private UUID evidenceId;

    @Column(name = "skill_id")
    private UUID skillId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EvidenceSkillId that = (EvidenceSkillId) o;
        return Objects.equals(evidenceId, that.evidenceId) && Objects.equals(skillId, that.skillId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(evidenceId, skillId);
    }
}
