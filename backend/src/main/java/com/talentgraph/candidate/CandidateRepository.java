package com.talentgraph.candidate;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CandidateRepository extends JpaRepository<Candidate, UUID> {

    List<Candidate> findByOrganizationId(UUID organizationId);

    Optional<Candidate> findByOrganizationIdAndEmailAndIsActiveTrue(UUID organizationId, String email);

    Optional<Candidate> findByIdAndOrganizationIdAndIsActiveTrue(UUID id, UUID organizationId);

    @Query("""
        SELECT c FROM Candidate c
        WHERE c.organization.id = :organizationId
          AND c.isActive = true
          AND (
            :search IS NULL OR :search = '' OR
            LOWER(c.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR
            LOWER(c.lastName) LIKE LOWER(CONCAT('%', :search, '%')) OR
            LOWER(c.email) LIKE LOWER(CONCAT('%', :search, '%')) OR
            (c.githubUsername IS NOT NULL AND LOWER(c.githubUsername) LIKE LOWER(CONCAT('%', :search, '%')))
          )
    """)
    Page<Candidate> searchCandidates(
            @Param("organizationId") UUID organizationId,
            @Param("search") String search,
            Pageable pageable
    );
}
