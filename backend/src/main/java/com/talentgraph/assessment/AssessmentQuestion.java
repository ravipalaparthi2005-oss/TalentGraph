package com.talentgraph.assessment;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "assessment_questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssessmentQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assessment_id", nullable = false)
    private Assessment assessment;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String constraints;

    @Column(name = "input_format", columnDefinition = "TEXT")
    private String inputFormat;

    @Column(name = "output_format", columnDefinition = "TEXT")
    private String outputFormat;

    @Column(name = "examples_json", columnDefinition = "TEXT")
    private String examplesJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Difficulty difficulty = Difficulty.MEDIUM;

    @Column(nullable = false)
    @Builder.Default
    private Integer points = 10;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 1;

    @Column(name = "time_limit_seconds", nullable = false)
    @Builder.Default
    private Double timeLimitSeconds = 2.0;

    @Column(name = "memory_limit_mb", nullable = false)
    @Builder.Default
    private Integer memoryLimitMb = 256;

    @Column(name = "allowed_languages_json", columnDefinition = "TEXT")
    private String allowedLanguagesJson;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
