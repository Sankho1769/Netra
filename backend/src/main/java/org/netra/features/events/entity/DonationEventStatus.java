package org.netra.features.events.entity;

public enum DonationEventStatus {
    DRAFT,
    PENDING_APPROVAL,
    PUBLISHED,
    REGISTRATION_CLOSED,
    ONGOING,
    COMPLETED,
    CANCELLED,
    REJECTED;

    public boolean isPubliclyVisible() {
        return this == PUBLISHED || this == REGISTRATION_CLOSED || this == ONGOING || this == COMPLETED;
    }
}
