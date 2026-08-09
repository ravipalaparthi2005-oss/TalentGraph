package com.talentgraph.assessment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TestCaseResponse {

    private UUID id;
    private UUID questionId;
    private String input;
    /** Expected output — NULL for candidates when testCaseType is HIDDEN */
    private String expectedOutput;
    private String testCaseType;
    private int displayOrder;
}
