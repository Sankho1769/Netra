package org.netra.features.karma.dto;

import org.netra.features.karma.entity.KarmaEventType;
import org.netra.features.karma.entity.KarmaTransaction;

import java.time.Instant;
import java.util.UUID;

public class KarmaTransactionDto {

    private UUID id;
    private int points;
    private KarmaEventType eventType;
    private String referenceType;
    private String referenceId;
    private String reason;
    private int previousBalance;
    private int resultingBalance;
    private Instant createdAt;

    public KarmaTransactionDto() {
    }

    public static KarmaTransactionDto fromEntity(KarmaTransaction entity) {
        if (entity == null) return null;
        KarmaTransactionDto dto = new KarmaTransactionDto();
        dto.setId(entity.getId());
        dto.setPoints(entity.getPoints());
        dto.setEventType(entity.getEventType());
        dto.setReferenceType(entity.getReferenceType());
        dto.setReferenceId(entity.getReferenceId());
        dto.setReason(entity.getReason());
        dto.setPreviousBalance(entity.getPreviousBalance());
        dto.setResultingBalance(entity.getResultingBalance());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
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

    public int getPreviousBalance() {
        return previousBalance;
    }

    public void setPreviousBalance(int previousBalance) {
        this.previousBalance = previousBalance;
    }

    public int getResultingBalance() {
        return resultingBalance;
    }

    public void setResultingBalance(int resultingBalance) {
        this.resultingBalance = resultingBalance;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
