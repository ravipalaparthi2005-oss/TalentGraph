package com.talentgraph.ai.analysis;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * AI-extracted skill proposal.
 * Validated against catalog before becoming an authoritative Evidence node.
 */
@Entity
@Table(name = "ai_extracted_skills")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiExtractedSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "run_id", nullable = false)
    private AiAnalysisRun run;

    @Column(name = "skill_name", nullable = false, length = 200)
    private String skillName;

    @Column(name = "evidence_quote", nullable = false, columnDefinition = "TEXT")
    private String evidenceQuote;

    @Column(name = "source_section", length = 100)
    private String sourceSection;

    /** Set when the skill is successfully matched to an existing catalog skill. */
    @Column(name = "catalog_skill_id")
    private UUID catalogSkillId;

    /** True after evidence quote has been verified in the source text and evidence node created. */
    @Column(nullable = false)
    @Builder.Default
    private boolean validated = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
