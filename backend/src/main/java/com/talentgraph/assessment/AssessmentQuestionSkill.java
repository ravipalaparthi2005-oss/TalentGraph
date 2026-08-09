package com.talentgraph.assessment;

import com.talentgraph.evidence.Skill;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "assessment_question_skills")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssessmentQuestionSkill {

    @EmbeddedId
    private AssessmentQuestionSkillId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("questionId")
    @JoinColumn(name = "question_id", nullable = false)
    private AssessmentQuestion question;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("skillId")
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;
}
