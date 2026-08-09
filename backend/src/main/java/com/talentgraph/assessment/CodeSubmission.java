package com.talentgraph.assessment;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "code_submissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodeSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attempt_id", nullable = false)
    private AssessmentAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private AssessmentQuestion question;

    @Column(nullable = false, length = 50)
    private String language;

    @Column(name = "source_code", nullable = false, columnDefinition = "TEXT")
    private String sourceCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private SubmissionStatus status = SubmissionStatus.SUBMITTED;

    @Column(name = "submitted_at", nullable = false)
    @Builder.Default
    private Instant submittedAt = Instant.now();

    @Column(name = "evaluated_at")
    private Instant evaluatedAt;

    @Column(name = "judge0_submission_token", length = 100)
    private String judge0SubmissionToken;

    @Column(name = "execution_time_ms")
    private Double executionTimeMs;

    @Column(name = "memory_kb")
    private Long memoryKb;

    @Column(name = "test_cases_passed", nullable = false)
    @Builder.Default
    private Integer testCasesPassed = 0;

    @Column(name = "test_cases_total", nullable = false)
    @Builder.Default
    private Integer testCasesTotal = 0;

    @Column(name = "judge0_status_id")
    private Integer judge0StatusId;

    @Column(name = "compiler_output", columnDefinition = "TEXT")
    private String compilerOutput;

    @Column(columnDefinition = "TEXT")
    private String stderr;

    @Column(columnDefinition = "TEXT")
    private String stdout;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
