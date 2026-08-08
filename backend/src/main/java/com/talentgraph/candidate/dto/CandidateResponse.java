package com.talentgraph.candidate.dto;

import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateResponse {

    private UUID id;
    private UUID organizationId;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String location;
    private String linkedinUrl;
    private String githubUsername;
    private String portfolioUrl;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
