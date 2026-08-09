package com.talentgraph.assessment;

import com.talentgraph.auth.User;
import com.talentgraph.candidate.Application;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "application_assessments", uniqueConstraints = {
        @UniqueConstraint(name = "uk_app_assessment", columnNames = {"application_id", "assessment_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assessment_id", nullable = false)
    private Assessment assessment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assigned_by_id", nullable = false)
    private User assignedBy;

    @Column(name = "assigned_at", nullable = false)
    @Builder.Default
    private Instant assignedAt = Instant.now();

    @Column(name = "due_at")
    private Instant dueAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ApplicationAssessmentStatus status = ApplicationAssessmentStatus.ASSIGNED;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
