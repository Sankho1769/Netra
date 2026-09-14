package org.netra.features.eligibility.repository;

import org.netra.features.eligibility.entity.EligibilityQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EligibilityQuestionRepository extends JpaRepository<EligibilityQuestion, UUID> {
    List<EligibilityQuestion> findByVersionAndActiveOrderByStepNumberAscSortOrderAsc(String version, boolean active);
    Optional<EligibilityQuestion> findByVersionAndQuestionKeyAndActive(String version, String questionKey, boolean active);
    boolean existsByVersionAndActive(String version, boolean active);
}
