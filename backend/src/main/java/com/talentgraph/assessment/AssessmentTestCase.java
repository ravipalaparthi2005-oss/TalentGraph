package com.talentgraph.assessment;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "assessment_test_cases")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssessmentTestCase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private AssessmentQuestion question;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String input;

    @Column(name = "expected_output", nullable = false, columnDefinition = "TEXT")
    private String expectedOutput;

    @Enumerated(EnumType.STRING)
    @Column(name = "test_case_type", nullable = false, length = 20)
    @Builder.Default
    private TestCaseType testCaseType = TestCaseType.HIDDEN;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 1;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
