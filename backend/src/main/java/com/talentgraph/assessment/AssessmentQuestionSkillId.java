package com.talentgraph.assessment;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class AssessmentQuestionSkillId implements Serializable {

    @Column(name = "question_id")
    private UUID questionId;

    @Column(name = "skill_id")
    private UUID skillId;
}
