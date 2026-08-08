package com.talentgraph.candidate.dto;

import com.talentgraph.candidate.ApplicationSource;
import com.talentgraph.candidate.ApplicationStatus;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationResponse {

    private UUID id;
    private UUID candidateId;
    private String candidateName;
    private String candidateEmail;
    private UUID jobId;
    private String jobTitle;
    private ApplicationStatus status;
    private ApplicationSource source;
    private Instant appliedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
