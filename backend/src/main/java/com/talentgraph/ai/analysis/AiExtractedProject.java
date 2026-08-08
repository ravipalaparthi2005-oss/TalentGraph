package com.talentgraph.ai.analysis;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "ai_extracted_projects")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AiExtractedProject {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "run_id", nullable = false)
    private AiAnalysisRun run;
    @Column(name = "project_name", nullable = false, length = 300)
    private String projectName;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(columnDefinition = "TEXT")
    private String technologies;
    @Column(name = "evidence_quote", nullable = false, columnDefinition = "TEXT")
    private String evidenceQuote;
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
