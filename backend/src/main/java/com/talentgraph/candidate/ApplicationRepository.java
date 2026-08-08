package com.talentgraph.candidate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, UUID> {
    List<Application> findByCandidateId(UUID candidateId);
    List<Application> findByJobId(UUID jobId);
    List<Application> findByJobIdAndStatus(UUID jobId, ApplicationStatus status);
    Optional<Application> findByCandidateIdAndJobId(UUID candidateId, UUID jobId);
}
