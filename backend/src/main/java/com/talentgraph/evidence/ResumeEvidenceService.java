package com.talentgraph.evidence;

import com.talentgraph.candidate.Candidate;
import com.talentgraph.document.CandidateDocument;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ResumeEvidenceService {

    private final EvidenceSourceRepository evidenceSourceRepository;
    private final EvidenceRepository evidenceRepository;
    private final EvidenceSkillRepository evidenceSkillRepository;
    private final SkillRepository skillRepository;

    @Transactional
    public EvidenceSource generateResumeEvidence(Candidate candidate, CandidateDocument document, String extractedText) {
        // 1. Create EvidenceSource for RESUME
        EvidenceSource source = EvidenceSource.builder()
                .candidate(candidate)
                .sourceType(EvidenceSourceType.RESUME)
                .externalReference(document.getOriginalFilename())
                .sourceUrl("/api/v1/candidates/" + candidate.getId() + "/documents/" + document.getId())
                .collectedAt(Instant.now())
                .metadataJson(String.format("{\"documentId\":\"%s\",\"sha256Hash\":\"%s\"}", document.getId(), document.getSha256Hash()))
                .build();
        source = evidenceSourceRepository.save(source);

        if (extractedText == null || extractedText.isBlank()) {
            return source;
        }

        // 2. Load catalog skills and perform phrase/token boundary matching
        List<Skill> catalogSkills = skillRepository.findAll();
        for (Skill skill : catalogSkills) {
            if (isSkillMentioned(extractedText, skill.getName(), skill.getNormalizedName())) {
                Evidence evidence = Evidence.builder()
                        .candidate(candidate)
                        .evidenceSource(source)
                        .title("Skill Observation: " + skill.getName())
                        .description("Explicit skill mention of '" + skill.getName() + "' detected in candidate resume document.")
                        .evidenceType(EvidenceType.SKILL_MENTION)
                        .observedValue(skill.getName())
                        .normalizedValue(skill.getNormalizedName())
                        .confidence(new BigDecimal("1.00"))
                        .occurredAt(Instant.now())
                        .sourceReference(document.getOriginalFilename())
                        .build();
                evidence = evidenceRepository.save(evidence);

                EvidenceSkill edge = EvidenceSkill.builder()
                        .id(new EvidenceSkillId(evidence.getId(), skill.getId()))
                        .evidence(evidence)
                        .skill(skill)
                        .relationshipType(EvidenceRelationshipType.DEMONSTRATES)
                        .build();
                evidenceSkillRepository.save(edge);
            }
        }

        return source;
    }

    private boolean isSkillMentioned(String text, String rawSkillName, String normalizedSkillName) {
        if (text == null || text.isBlank() || normalizedSkillName == null || normalizedSkillName.isBlank()) {
            return false;
        }

        // Single letter skills like "C" require exact word boundary & case awareness
        if (normalizedSkillName.length() == 1) {
            Pattern pattern = Pattern.compile("\\b" + Pattern.quote(rawSkillName) + "\\b");
            return pattern.matcher(text).find();
        }

        String[] words = normalizedSkillName.trim().split("\\s+");
        StringBuilder regex = new StringBuilder("\\b");
        for (int i = 0; i < words.length; i++) {
            if (i > 0) regex.append("\\s+");
            regex.append(Pattern.quote(words[i]));
        }
        regex.append("\\b");
        Pattern pattern = Pattern.compile(regex.toString(), Pattern.CASE_INSENSITIVE);
        return pattern.matcher(text).find();
    }
}
