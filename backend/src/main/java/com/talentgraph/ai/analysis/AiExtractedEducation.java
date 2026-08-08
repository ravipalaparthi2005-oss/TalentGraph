package com.talentgraph.ai.analysis;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;
import java.util.UUID;

@Entity @Table(name = "ai_extracted_education")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AiExtractedEducation {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "run_id", nullable = false)
    private AiAnalysisRun run;
    @Column(length = 300)
    private String institution;
    @Column(length = 200)
    private String degree;
    @Column(name = "field_of_study", length = 200)
    private String fieldOfStudy;
    @Column(name = "start_date", length = 50)
    private String startDate;
    @Column(name = "end_date", length = 50)
    private String endDate;
    @Column(length = 50)
    private String grade;
    @Column(name = "evidence_quote", nullable = false, columnDefinition = "TEXT")
    private String evidenceQuote;
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
