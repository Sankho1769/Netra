package org.netra.features.events.entity;

public enum DonationEventRegistrationStatus {
    REGISTERED,
    WAITLISTED,
    CANCELLED,
    CHECKED_IN,
    COMPLETED,
    NO_SHOW,
    REJECTED;

    /**
     * Architectural invariant:
     * REGISTERED and CHECKED_IN consume a capacity slot.
     * CANCELLED, REJECTED, and NO_SHOW do not consume a capacity slot.
     * COMPLETED remains historical and does not consume current capacity.
     */
    public boolean consumesCapacitySlot() {
        return this == REGISTERED || this == CHECKED_IN;
    }
}
