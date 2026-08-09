package com.talentgraph.assessment.service;

import com.talentgraph.auth.User;
import com.talentgraph.common.exception.ResourceNotFoundException;
import com.talentgraph.organization.OrganizationRole;
import com.talentgraph.organization.service.OrganizationAuthorizationService;
import com.talentgraph.assessment.*;
import com.talentgraph.assessment.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Service managing test cases (PUBLIC & HIDDEN).
 *
 * <p>Security rule: Hidden expected outputs MUST NEVER be exposed in candidate APIs.
 */
@Service
@RequiredArgsConstructor
public class TestCaseManagementService {

    private final AssessmentQuestionRepository questionRepository;
    private final AssessmentTestCaseRepository testCaseRepository;
    private final OrganizationAuthorizationService authorizationService;

    /**
     * Add a test case to a coding question.
     */
    @Transactional
    public TestCaseResponse addTestCase(UUID orgId, UUID questionId, CreateTestCaseRequest request, User currentUser) {
        authorizationService.requireRole(currentUser, orgId, OrganizationRole.RECRUITER);

        AssessmentQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + questionId));

        if (!question.getAssessment().getOrganization().getId().equals(orgId)) {
            throw new IllegalArgumentException("Question belongs to another organization.");
        }

        TestCaseType type = TestCaseType.HIDDEN;
        if (request.getTestCaseType() != null) {
            try { type = TestCaseType.valueOf(request.getTestCaseType().toUpperCase()); } catch (Exception ignored) {}
        }

        AssessmentTestCase testCase = AssessmentTestCase.builder()
                .question(question)
                .input(request.getInput().strip())
                .expectedOutput(request.getExpectedOutput().strip())
                .testCaseType(type)
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 1)
                .build();

        testCase = testCaseRepository.save(testCase);

        return TestCaseResponse.builder()
                .id(testCase.getId())
                .questionId(questionId)
                .input(testCase.getInput())
                .expectedOutput(testCase.getExpectedOutput())
                .testCaseType(testCase.getTestCaseType().name())
                .displayOrder(testCase.getDisplayOrder())
                .build();
    }

    /**
     * Get test cases for a question.
     */
    @Transactional(readOnly = true)
    public List<TestCaseResponse> getTestCases(UUID orgId, UUID questionId, boolean includeHidden, User currentUser) {
        if (currentUser != null && orgId != null) {
            authorizationService.requireRole(currentUser, orgId, OrganizationRole.RECRUITER);
        }

        AssessmentQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + questionId));

        List<AssessmentTestCase> list;
        if (includeHidden) {
            list = testCaseRepository.findByQuestionIdOrderByDisplayOrderAsc(questionId);
        } else {
            list = testCaseRepository.findByQuestionIdAndTestCaseTypeOrderByDisplayOrderAsc(questionId, TestCaseType.PUBLIC);
        }

        return list.stream().map(tc -> TestCaseResponse.builder()
                .id(tc.getId())
                .questionId(questionId)
                .input(tc.getInput())
                .expectedOutput(tc.getTestCaseType() == TestCaseType.PUBLIC || includeHidden ? tc.getExpectedOutput() : null)
                .testCaseType(tc.getTestCaseType().name())
                .displayOrder(tc.getDisplayOrder())
                .build()).toList();
    }
}
