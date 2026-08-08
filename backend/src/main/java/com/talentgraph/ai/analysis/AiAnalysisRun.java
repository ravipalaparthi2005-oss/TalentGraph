package com.talentgraph.ai.analysis;

import com.talentgraph.document.CandidateDocument;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

/**
 * Persisted record of a single AI analysis run against a candidate document.
 *
 * <p>Every invocation of the AI provider creates a new run record, enabling
 * full auditability: who triggered analysis, which model, which prompt version,
 * what input hash, and whether it succeeded or failed.
 *
 * <p>API keys, full prompts, and resume text are NEVER stored in this table.
 */
@Entity
@Table(name = "ai_analysis_runs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiAnalysisRun {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private CandidateDocument document;

    @Column(nullable = false, length = 50)
    private String provider;

    @Column(nullable = false, length = 200)
    private String model;

    @Column(name = "analysis_type", nullable = false, length = 50)
    private String analysisType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AiAnalysisStatus status = AiAnalysisStatus.PENDING;

    @Column(name = "prompt_version", nullable = false, length = 50)
    private String promptVersion;

    /**
     * SHA-256 hash of the parsed resume text content.
     * Used for idempotency: same content + same type + same prompt version = skip re-analysis.
     */
    @Column(name = "input_hash", nullable = false, length = 64)
    private String inputHash;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "error_code", length = 50)
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
