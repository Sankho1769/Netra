package org.netra.features.bloodrequest.entity;

/**
 * Lifecycle status for blood requests.
 * Supports the full authoritative lifecycle:
 * OPEN, VERIFIED, FULFILLED, CANCELLED, EXPIRED, FLAGGED, CONFIRMED_FAKE, REJECTED.
 */
public enum BloodRequestStatus {
    OPEN,
    VERIFIED,
    FULFILLED,
    CANCELLED,
    EXPIRED,
    FLAGGED,
    CONFIRMED_FAKE,
    REJECTED;

    /**
     * Active discoverable requests for blood donation.
     */
    public boolean isDiscoverable() {
        return this == OPEN || this == VERIFIED;
    }

    public boolean isTerminal() {
        return this == FULFILLED || this == CANCELLED || this == EXPIRED || this == CONFIRMED_FAKE || this == REJECTED;
    }
}
