package com.talentgraph.assessment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CodingLanguageRepository extends JpaRepository<CodingLanguage, UUID> {

    List<CodingLanguage> findByIsEnabledTrue();

    Optional<CodingLanguage> findBySlugAndIsEnabledTrue(String slug);

    Optional<CodingLanguage> findByJudge0LanguageId(Integer judge0LanguageId);
}
