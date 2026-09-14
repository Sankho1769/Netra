package org.netra.features.eligibility.repository;

import org.netra.features.eligibility.entity.EligibilityAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EligibilityAnswerRepository extends JpaRepository<EligibilityAnswer, UUID> {
    List<EligibilityAnswer> findBySessionId(UUID sessionId);
    Optional<EligibilityAnswer> findBySessionIdAndQuestionKey(UUID sessionId, String questionKey);
    void deleteBySessionId(UUID sessionId);
}
