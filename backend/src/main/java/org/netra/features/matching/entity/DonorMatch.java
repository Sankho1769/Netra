package org.netra.features.matching.entity;

import jakarta.persistence.*;
import org.netra.core.exception.ValidationException;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * JPA entity representing a persistent, stateful donor match.
 *
 * Links a blood request to an individual eligible donor user.
 * Supports optimistic concurrency control via {@code @Version}.
 */
@Entity
@Table(name = "donor_matches", uniqueConstraints = {
        @UniqueConstraint(name = "uq_donor_matches_request_donor", columnNames = {"blood_request_id", "donor_user_id"})
})
public class DonorMatch {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "blood_request_id", nullable = false, updatable = false)
    private UUID bloodRequestId;

    @Column(name = "donor_user_id", nullable = false, updatable = false)
    private UUID donorUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "response_status", nullable = false, length = 50)
    private MatchStatus responseStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "responded_at")
    private Instant respondedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    public DonorMatch() {
    }

    public DonorMatch(UUID bloodRequestId, UUID donorUserId, Instant expiresAt) {
        this(UUID.randomUUID(), bloodRequestId, donorUserId, MatchStatus.MATCHED, Instant.now(), expiresAt);
    }

    public DonorMatch(UUID bloodRequestId, UUID donorUserId, Instant now, Instant expiresAt) {
        this(UUID.randomUUID(), bloodRequestId, donorUserId, MatchStatus.MATCHED, now, expiresAt);
    }

    public DonorMatch(UUID id, UUID bloodRequestId, UUID donorUserId, MatchStatus responseStatus, Instant now, Instant expiresAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.bloodRequestId = Objects.requireNonNull(bloodRequestId, "bloodRequestId must not be null");
        this.donorUserId = Objects.requireNonNull(donorUserId, "donorUserId must not be null");
        this.responseStatus = responseStatus != null ? responseStatus : MatchStatus.MATCHED;
        this.createdAt = now != null ? now : Instant.now();
        this.updatedAt = this.createdAt;
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        this.version = 0L;
    }

    /**
     * Checks if the match has reached its expiration timestamp.
     * Expiration condition: expiresAt <= now.
     */
    public boolean isExpired(Instant now) {
        return this.expiresAt.compareTo(now) <= 0;
    }

    /**
     * Transitions status from MATCHED to ACCEPTED.
     */
    public void accept(Instant now) {
        if (!responseStatus.canTransitionTo(MatchStatus.ACCEPTED)) {
            throw new ValidationException("Cannot accept match in status: " + responseStatus);
        }
        if (isExpired(now)) {
            throw new ValidationException("Cannot accept an expired match.");
        }
        this.responseStatus = MatchStatus.ACCEPTED;
        this.respondedAt = now;
        this.updatedAt = now;
    }

    /**
     * Transitions status from MATCHED to DECLINED.
     */
    public void decline(Instant now) {
        if (!responseStatus.canTransitionTo(MatchStatus.DECLINED)) {
            throw new ValidationException("Cannot decline match in status: " + responseStatus);
        }
        if (isExpired(now)) {
            throw new ValidationException("Cannot decline an expired match.");
        }
        this.responseStatus = MatchStatus.DECLINED;
        this.respondedAt = now;
        this.updatedAt = now;
    }

    /**
     * Transitions status from MATCHED to EXPIRED.
     */
    public void expire(Instant now) {
        if (!responseStatus.canTransitionTo(MatchStatus.EXPIRED)) {
            throw new ValidationException("Cannot expire match in status: " + responseStatus);
        }
        this.responseStatus = MatchStatus.EXPIRED;
        this.updatedAt = now;
    }

    /**
     * Transitions status from MATCHED to CANCELLED.
     */
    public void cancel(Instant now) {
        if (!responseStatus.canTransitionTo(MatchStatus.CANCELLED)) {
            throw new ValidationException("Cannot cancel match in status: " + responseStatus);
        }
        this.responseStatus = MatchStatus.CANCELLED;
        this.updatedAt = now;
    }

    // --- Getters & Safe Domain Mutators ---

    public UUID getId() {
        return id;
    }

    public UUID getBloodRequestId() {
        return bloodRequestId;
    }

    public UUID getDonorUserId() {
        return donorUserId;
    }

    public MatchStatus getResponseStatus() {
        return responseStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getRespondedAt() {
        return respondedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Long getVersion() {
        return version;
    }
}
