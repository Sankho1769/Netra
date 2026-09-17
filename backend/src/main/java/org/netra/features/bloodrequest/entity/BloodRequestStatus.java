package org.netra.features.bloodrequest.entity;

/**
 * Lifecycle status for blood requests in V1.
 *
 * NOTE ON V1 STATUS MODEL:
 * The initial V1 system defines OPEN, FULFILLED, CANCELLED, and EXPIRED.
 * We deliberately DO NOT implement PARTIALLY_FULFILLED in V1 because there is no units_fulfilled
 * field and no active fulfillment/reservation subsystem yet. Future donor matching/verified donation
 * phases may introduce fulfillment records and PARTIALLY_FULFILLED, but that is out of scope for V1.
 */
public enum BloodRequestStatus {
    OPEN,
    FULFILLED,
    CANCELLED,
    EXPIRED;

    /**
     * In V1, only OPEN requests are discoverable for active blood donation.
     */
    public boolean isDiscoverable() {
        return this == OPEN;
    }

    public boolean isTerminal() {
        return this == FULFILLED || this == CANCELLED || this == EXPIRED;
    }
}
