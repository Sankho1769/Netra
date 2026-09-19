package org.netra.features.matching.repository;

import org.netra.features.matching.entity.DonorMatch;
import org.netra.features.matching.entity.MatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DonorMatchRepository extends JpaRepository<DonorMatch, UUID> {

    List<DonorMatch> findByDonorUserIdOrderByCreatedAtDesc(UUID donorUserId);

    Optional<DonorMatch> findByIdAndDonorUserId(UUID id, UUID donorUserId);

    List<DonorMatch> findByBloodRequestIdOrderByCreatedAtDesc(UUID bloodRequestId);

    Optional<DonorMatch> findByBloodRequestIdAndDonorUserId(UUID bloodRequestId, UUID donorUserId);

    boolean existsByBloodRequestIdAndDonorUserId(UUID bloodRequestId, UUID donorUserId);

    int countByBloodRequestIdAndResponseStatus(UUID bloodRequestId, MatchStatus responseStatus);

    /**
     * Atomic compare-and-set query for donor accept/decline.
     * Prevents race conditions and lost updates without relying solely on application read-then-write.
     *
     * @return 1 if successfully transitioned, 0 if match already transitioned or expired.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE DonorMatch m SET m.responseStatus = :newStatus, m.respondedAt = :now, m.updatedAt = :now, m.version = m.version + 1 " +
           "WHERE m.id = :id AND m.donorUserId = :donorId AND m.responseStatus = :expectedStatus AND m.expiresAt > :now")
    int atomicTransitionStatus(
            @Param("id") UUID id,
            @Param("donorId") UUID donorId,
            @Param("expectedStatus") MatchStatus expectedStatus,
            @Param("newStatus") MatchStatus newStatus,
            @Param("now") Instant now
    );

    /**
     * Batch atomic expiration of overdue matches where responseStatus == MATCHED and either
     * expiresAt <= :now OR the parent blood request is in EXPIRED status.
     * Excludes matches whose parent blood request has been CANCELLED to prevent lifecycle race.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE DonorMatch m SET m.responseStatus = org.netra.features.matching.entity.MatchStatus.EXPIRED, m.updatedAt = :now, m.version = m.version + 1 " +
           "WHERE m.responseStatus = org.netra.features.matching.entity.MatchStatus.MATCHED AND " +
           "m.bloodRequestId NOT IN (SELECT r.id FROM BloodRequest r WHERE r.status = org.netra.features.bloodrequest.entity.BloodRequestStatus.CANCELLED) AND " +
           "(m.expiresAt <= :now OR m.bloodRequestId IN (SELECT r.id FROM BloodRequest r WHERE r.status = org.netra.features.bloodrequest.entity.BloodRequestStatus.EXPIRED))")
    int expireOverdueMatches(@Param("now") Instant now);

    /**
     * Batch atomic cancellation of active MATCHED matches for a specific cancelled request.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE DonorMatch m SET m.responseStatus = org.netra.features.matching.entity.MatchStatus.CANCELLED, m.updatedAt = :now, m.version = m.version + 1 " +
           "WHERE m.bloodRequestId = :requestId AND m.responseStatus = org.netra.features.matching.entity.MatchStatus.MATCHED")
    int cancelActiveMatchesForRequest(@Param("requestId") UUID requestId, @Param("now") Instant now);

    /**
     * Batch atomic expiration of active MATCHED matches for a specific expired request.
     * Conditional on the parent blood request being in EXPIRED status.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE DonorMatch m SET m.responseStatus = org.netra.features.matching.entity.MatchStatus.EXPIRED, m.updatedAt = :now, m.version = m.version + 1 " +
           "WHERE m.bloodRequestId = :requestId AND m.responseStatus = org.netra.features.matching.entity.MatchStatus.MATCHED AND " +
           "m.bloodRequestId IN (SELECT r.id FROM BloodRequest r WHERE r.status = org.netra.features.bloodrequest.entity.BloodRequestStatus.EXPIRED)")
    int expireActiveMatchesForRequest(@Param("requestId") UUID requestId, @Param("now") Instant now);
}
