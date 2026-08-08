package com.talentgraph.evidence;

import com.talentgraph.common.exception.DuplicateResourceException;
import com.talentgraph.common.exception.ResourceNotFoundException;
import com.talentgraph.evidence.dto.CreateSkillRequest;
import com.talentgraph.evidence.dto.SkillResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class SkillService {

    private final SkillRepository skillRepository;

    public SkillService(SkillRepository skillRepository) {
        this.skillRepository = skillRepository;
    }

    public static String normalizeSkillName(String rawName) {
        if (rawName == null) {
            return "";
        }
        return rawName.trim().toLowerCase().replaceAll("\\s+", " ");
    }

    @Transactional
    public SkillResponse createSkill(CreateSkillRequest request) {
        String normalized = normalizeSkillName(request.getName());
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Skill name cannot be blank");
        }

        if (skillRepository.existsByNormalizedName(normalized)) {
            throw new DuplicateResourceException("Skill already exists with normalized name: '" + normalized + "'");
        }

        SkillCategory category = request.getCategory() != null ? request.getCategory() : SkillCategory.OTHER;

        Skill skill = Skill.builder()
                .name(request.getName().trim())
                .normalizedName(normalized)
                .category(category)
                .build();

        Skill saved = skillRepository.save(skill);
        return SkillResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public List<SkillResponse> getSkills(String search) {
        List<Skill> skills;
        if (search != null && !search.isBlank()) {
            String query = search.trim();
            skills = skillRepository.findByNameContainingIgnoreCaseOrNormalizedNameContainingIgnoreCase(query, query.toLowerCase());
        } else {
            skills = skillRepository.findAll();
        }

        return skills.stream()
                .map(SkillResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public SkillResponse getSkillById(UUID skillId) {
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> new ResourceNotFoundException("Skill with ID " + skillId + " not found"));
        return SkillResponse.fromEntity(skill);
    }
}
