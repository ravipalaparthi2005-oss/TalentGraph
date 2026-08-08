package com.talentgraph.ai.analysis;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_extracted_experiences")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AiExtractedExperience {

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "run_id", nullable = false)
    private AiAnalysisRun run;

    @Column(length = 300)
    private String company;

    @Column(name = "job_title", length = 300)
    private String jobTitle;

    @Column(name = "start_date", length = 50)
    private String startDate;

    @Column(name = "end_date", length = 50)
    private String endDate;

    @Column(columnDefinition = "TEXT")
    private String responsibilities;

    @Column(name = "evidence_quote", nullable = false, columnDefinition = "TEXT")
    private String evidenceQuote;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
