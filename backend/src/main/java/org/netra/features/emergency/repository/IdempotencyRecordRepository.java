package org.netra.features.emergency.repository;

import jakarta.persistence.LockModeType;
import org.netra.features.emergency.entity.IdempotencyRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, UUID> {

    Optional<IdempotencyRecord> findByUserIdAndIdempotencyKeyAndResourceType(
            UUID userId, String idempotencyKey, String resourceType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM IdempotencyRecord r WHERE r.userId = :userId AND r.idempotencyKey = :key AND r.resourceType = :resourceType")
    Optional<IdempotencyRecord> findForUpdate(
            @Param("userId") UUID userId,
            @Param("key") String key,
            @Param("resourceType") String resourceType);

    @Modifying
    @Query("DELETE FROM IdempotencyRecord r WHERE r.userId = :userId AND r.idempotencyKey = :key AND r.resourceType = :resourceType AND r.expiresAt < :now")
    int deleteExpiredRecord(
            @Param("userId") UUID userId,
            @Param("key") String key,
            @Param("resourceType") String resourceType,
            @Param("now") Instant now);

    long deleteByExpiresAtBefore(Instant now);
}
