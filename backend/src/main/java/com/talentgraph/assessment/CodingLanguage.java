package com.talentgraph.assessment;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "coding_languages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodingLanguage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Column(name = "judge0_language_id", nullable = false, unique = true)
    private Integer judge0LanguageId;

    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    private Boolean isEnabled = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
