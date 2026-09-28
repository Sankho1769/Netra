package org.netra.features.karma.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "karma_transactions")
public class KarmaTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "points", nullable = false)
    private Integer points;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 64)
    private KarmaEventType eventType;

    @Column(name = "reference_type", length = 64)
    private String referenceType;

    @Column(name = "reference_id", length = 128)
    private String referenceId;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Column(name = "actor_id")
    private UUID actorId;

    @Column(name = "previous_balance", nullable = false)
    private Integer previousBalance;

    @Column(name = "resulting_balance", nullable = false)
    private Integer resultingBalance;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public KarmaTransaction() {
    }

    public KarmaTransaction(
            UUID accountId,
            UUID userId,
            Integer points,
            KarmaEventType eventType,
            String referenceType,
            String referenceId,
            String reason,
            UUID actorId,
            Integer previousBalance,
            Integer resultingBalance) {
        this.accountId = accountId;
        this.userId = userId;
        this.points = points;
        this.eventType = eventType;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.reason = reason;
        this.actorId = actorId;
        this.previousBalance = previousBalance;
        this.resultingBalance = resultingBalance;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public Integer getPoints() {
        return points;
    }

    public void setPoints(Integer points) {
        this.points = points;
    }

    public KarmaEventType getEventType() {
        return eventType;
    }

    public void setEventType(KarmaEventType eventType) {
        this.eventType = eventType;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public UUID getActorId() {
        return actorId;
    }

    public void setActorId(UUID actorId) {
        this.actorId = actorId;
    }

    public Integer getPreviousBalance() {
        return previousBalance;
    }

    public void setPreviousBalance(Integer previousBalance) {
        this.previousBalance = previousBalance;
    }

    public Integer getResultingBalance() {
        return resultingBalance;
    }

    public void setResultingBalance(Integer resultingBalance) {
        this.resultingBalance = resultingBalance;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
