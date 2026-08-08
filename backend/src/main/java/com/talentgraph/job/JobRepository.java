package com.talentgraph.job;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JobRepository extends JpaRepository<Job, UUID> {
    List<Job> findByOrganizationId(UUID organizationId);
    List<Job> findByOrganizationIdAndStatus(UUID organizationId, JobStatus status);

    @Query("SELECT j FROM Job j WHERE j.organization.id = :orgId " +
           "AND (:status IS NULL OR j.status = :status) " +
           "AND (:employmentType IS NULL OR j.employmentType = :employmentType) " +
           "AND (:search IS NULL OR LOWER(j.title) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(j.department) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "OR LOWER(j.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Job> findJobsFiltered(
            @Param("orgId") UUID orgId,
            @Param("status") JobStatus status,
            @Param("employmentType") EmploymentType employmentType,
            @Param("search") String search,
            Pageable pageable
    );
}
