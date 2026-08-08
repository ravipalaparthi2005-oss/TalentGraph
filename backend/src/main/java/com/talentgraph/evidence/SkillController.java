package com.talentgraph.evidence;

import com.talentgraph.common.ApiResponse;
import com.talentgraph.evidence.dto.CreateSkillRequest;
import com.talentgraph.evidence.dto.SkillResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/skills")
public class SkillController {

    private final SkillService skillService;

    public SkillController(SkillService skillService) {
        this.skillService = skillService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SkillResponse>> createSkill(@Valid @RequestBody CreateSkillRequest request) {
        SkillResponse response = skillService.createSkill(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Skill created successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SkillResponse>>> getSkills(@RequestParam(required = false) String search) {
        List<SkillResponse> skills = skillService.getSkills(search);
        return ResponseEntity.ok(ApiResponse.success(skills));
    }

    @GetMapping("/{skillId}")
    public ResponseEntity<ApiResponse<SkillResponse>> getSkillById(@PathVariable UUID skillId) {
        SkillResponse skill = skillService.getSkillById(skillId);
        return ResponseEntity.ok(ApiResponse.success(skill));
    }
}
