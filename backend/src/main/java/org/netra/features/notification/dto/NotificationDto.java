package org.netra.features.notification.dto;

import org.netra.features.notification.entity.Notification;
import org.netra.features.notification.entity.NotificationDeliveryStatus;
import org.netra.features.notification.entity.NotificationReferenceType;
import org.netra.features.notification.entity.NotificationType;

import java.time.Instant;
import java.util.UUID;

public class NotificationDto {

    private UUID id;
    private NotificationType type;
    private String title;
    private String body;
    private NotificationReferenceType referenceType;
    private UUID referenceId;
    private Instant createdAt;
    private Instant readAt;
    private boolean read;
    private NotificationDeliveryStatus deliveryStatus;

    public NotificationDto() {
    }

    public NotificationDto(
            UUID id,
            NotificationType type,
            String title,
            String body,
            NotificationReferenceType referenceType,
            UUID referenceId,
            Instant createdAt,
            Instant readAt,
            boolean read,
            NotificationDeliveryStatus deliveryStatus) {
        this.id = id;
        this.type = type;
        this.title = title;
        this.body = body;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.createdAt = createdAt;
        this.readAt = readAt;
        this.read = read;
        this.deliveryStatus = deliveryStatus;
    }

    public static NotificationDto fromEntity(Notification entity) {
        if (entity == null) {
            return null;
        }
        return new NotificationDto(
                entity.getId(),
                entity.getType(),
                entity.getTitle(),
                entity.getBody(),
                entity.getReferenceType(),
                entity.getReferenceId(),
                entity.getCreatedAt(),
                entity.getReadAt(),
                entity.isRead(),
                entity.getDeliveryStatus()
        );
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public NotificationReferenceType getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(NotificationReferenceType referenceType) {
        this.referenceType = referenceType;
    }

    public UUID getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(UUID referenceId) {
        this.referenceId = referenceId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public void setReadAt(Instant readAt) {
        this.readAt = readAt;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public NotificationDeliveryStatus getDeliveryStatus() {
        return deliveryStatus;
    }

    public void setDeliveryStatus(NotificationDeliveryStatus deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }
}
