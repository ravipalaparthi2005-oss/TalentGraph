package com.talentgraph.github.repository;

import com.talentgraph.github.GithubRepositoryLanguage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GithubRepositoryLanguageRepository extends JpaRepository<GithubRepositoryLanguage, UUID> {

    List<GithubRepositoryLanguage> findByRepositoryIdOrderByBytesCountDesc(UUID repositoryId);

    Optional<GithubRepositoryLanguage> findByRepositoryIdAndLanguageName(UUID repositoryId, String languageName);
}
