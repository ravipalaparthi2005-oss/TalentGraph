package com.talentgraph.assessment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talentgraph.auth.User;
import com.talentgraph.common.exception.ResourceNotFoundException;
import com.talentgraph.evidence.Skill;
import com.talentgraph.evidence.SkillRepository;
import com.talentgraph.organization.OrganizationRole;
import com.talentgraph.organization.service.OrganizationAuthorizationService;
import com.talentgraph.assessment.*;
import com.talentgraph.assessment.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service managing coding questions and skill mappings.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuestionManagementService {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentQuestionRepository questionRepository;
    private final AssessmentTestCaseRepository testCaseRepository;
    private final AssessmentQuestionSkillRepository questionSkillRepository;
    private final SkillRepository skillRepository;
    private final OrganizationAuthorizationService authorizationService;
    private final ObjectMapper objectMapper;

    /**
     * Add a coding question to an assessment.
     */
    @Transactional
    public QuestionResponse addQuestion(UUID orgId, UUID assessmentId, CreateQuestionRequest request, User currentUser) {
        authorizationService.requireRole(currentUser, orgId, OrganizationRole.RECRUITER);

        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment not found: " + assessmentId));

        if (!assessment.getOrganization().getId().equals(orgId)) {
            throw new IllegalArgumentException("Assessment belongs to another organization.");
        }

        String allowedLangsJson = null;
        if (request.getAllowedLanguages() != null && !request.getAllowedLanguages().isEmpty()) {
            try {
                allowedLangsJson = objectMapper.writeValueAsString(request.getAllowedLanguages());
            } catch (Exception e) {
                log.warn("Failed to serialize allowed languages", e);
            }
        }

        Difficulty diff = Difficulty.MEDIUM;
        if (request.getDifficulty() != null) {
            try { diff = Difficulty.valueOf(request.getDifficulty().toUpperCase()); } catch (Exception ignored) {}
        }

        AssessmentQuestion question = AssessmentQuestion.builder()
                .assessment(assessment)
                .title(request.getTitle().strip())
                .description(request.getDescription())
                .constraints(request.getConstraints())
                .inputFormat(request.getInputFormat())
                .outputFormat(request.getOutputFormat())
                .examplesJson(request.getExamplesJson())
                .difficulty(diff)
                .points(request.getPoints() != null ? request.getPoints() : 10)
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 1)
                .timeLimitSeconds(request.getTimeLimitSeconds() != null ? request.getTimeLimitSeconds() : 2.0)
                .memoryLimitMb(request.getMemoryLimitMb() != null ? request.getMemoryLimitMb() : 256)
                .allowedLanguagesJson(allowedLangsJson)
                .build();

        question = questionRepository.save(question);

        // Map skills
        if (request.getSkillIds() != null) {
            for (UUID skillId : request.getSkillIds()) {
                Skill skill = skillRepository.findById(skillId).orElse(null);
                if (skill != null) {
                    questionSkillRepository.save(AssessmentQuestionSkill.builder()
                            .id(new AssessmentQuestionSkillId(question.getId(), skill.getId()))
                            .question(question)
                            .skill(skill)
                            .build());
                }
            }
        }

        return toResponse(question, true);
    }

    /**
     * Get questions for an assessment.
     */
    @Transactional(readOnly = true)
    public List<QuestionResponse> getQuestions(UUID orgId, UUID assessmentId, boolean includeHiddenTestCases, User currentUser) {
        authorizationService.requireRole(currentUser, orgId, OrganizationRole.RECRUITER);

        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assessment not found: " + assessmentId));

        if (!assessment.getOrganization().getId().equals(orgId)) {
            throw new IllegalArgumentException("Assessment belongs to another organization.");
        }

        List<AssessmentQuestion> questions = questionRepository.findByAssessmentIdOrderByDisplayOrderAsc(assessmentId);
        return questions.stream().map(q -> toResponse(q, includeHiddenTestCases)).toList();
    }

    private QuestionResponse toResponse(AssessmentQuestion q, boolean includeHiddenTestCases) {
        List<AssessmentTestCase> testCases;
        if (includeHiddenTestCases) {
            testCases = testCaseRepository.findByQuestionIdOrderByDisplayOrderAsc(q.getId());
        } else {
            testCases = testCaseRepository.findByQuestionIdAndTestCaseTypeOrderByDisplayOrderAsc(q.getId(), TestCaseType.PUBLIC);
        }

        List<TestCaseResponse> tcResponses = testCases.stream().map(tc -> TestCaseResponse.builder()
                .id(tc.getId())
                .questionId(q.getId())
                .input(tc.getInput())
                .expectedOutput(tc.getTestCaseType() == TestCaseType.PUBLIC || includeHiddenTestCases ? tc.getExpectedOutput() : null)
                .testCaseType(tc.getTestCaseType().name())
                .displayOrder(tc.getDisplayOrder())
                .build()).toList();

        List<AssessmentQuestionSkill> qSkills = questionSkillRepository.findByQuestionId(q.getId());
        List<QuestionResponse.SkillDto> skillDtos = qSkills.stream().map(qs -> QuestionResponse.SkillDto.builder()
                .id(qs.getSkill().getId())
                .name(qs.getSkill().getName())
                .normalizedName(qs.getSkill().getNormalizedName())
                .category(qs.getSkill().getCategory() != null ? qs.getSkill().getCategory().name() : null)
                .build()).toList();

        List<String> allowedLangs = new ArrayList<>();
        if (q.getAllowedLanguagesJson() != null) {
            try {
                allowedLangs = objectMapper.readValue(q.getAllowedLanguagesJson(), new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {});
            } catch (Exception ignored) {}
        }

        return QuestionResponse.builder()
                .id(q.getId())
                .assessmentId(q.getAssessment().getId())
                .title(q.getTitle())
                .description(q.getDescription())
                .constraints(q.getConstraints())
                .inputFormat(q.getInputFormat())
                .outputFormat(q.getOutputFormat())
                .examplesJson(q.getExamplesJson())
                .difficulty(q.getDifficulty().name())
                .points(q.getPoints())
                .displayOrder(q.getDisplayOrder())
                .timeLimitSeconds(q.getTimeLimitSeconds())
                .memoryLimitMb(q.getMemoryLimitMb())
                .allowedLanguages(allowedLangs)
                .skills(skillDtos)
                .testCases(tcResponses)
                .build();
    }
}
