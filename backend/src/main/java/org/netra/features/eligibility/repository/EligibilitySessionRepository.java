package org.netra.features.eligibility.repository;

import org.netra.features.eligibility.entity.EligibilitySession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EligibilitySessionRepository extends JpaRepository<EligibilitySession, UUID> {
    Optional<EligibilitySession> findByIdAndUserId(UUID id, UUID userId);
    List<EligibilitySession> findByUserIdOrderByStartedAtDesc(UUID userId);
    List<EligibilitySession> findByUserIdInOrderByStartedAtDesc(java.util.Collection<UUID> userIds);
    List<EligibilitySession> findByUserIdInAndStatusOrderByCompletedAtDesc(java.util.Collection<UUID> userIds, org.netra.features.eligibility.entity.SessionStatus status);
    List<EligibilitySession> findByUserIdAndStatusOrderByCompletedAtDesc(UUID userId, org.netra.features.eligibility.entity.SessionStatus status);
    List<EligibilitySession> findByExpiresAtBeforeAndCompletedAtIsNull(Instant cutoff);
}
