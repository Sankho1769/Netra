package org.netra.features.matching.entity;

/**
 * State lifecycle for persistent donor matches.
 *
 * Full authoritative lifecycle:
 * MATCHED -> ACCEPTED
 * MATCHED -> DECLINED
 * MATCHED -> EXPIRED
 * MATCHED -> CANCELLED
 *
 * ACCEPTED -> ARRIVED
 * ACCEPTED -> CANCELLED / CANCELLED_SAFE (legitimate cancellation)
 * ACCEPTED -> CONFIRMED_NO_SHOW (authoritative staff/admin confirmation only)
 *
 * ARRIVED -> MEDICAL_REJECTION (medical rejection does NOT penalize karma)
 */
public enum MatchStatus {
    MATCHED,
    ACCEPTED,
    DECLINED,
    EXPIRED,
    CANCELLED,
    ARRIVED,
    CONFIRMED_NO_SHOW,
    MEDICAL_REJECTION,
    CANCELLED_SAFE;

    /**
     * Determines whether the status represents a final, immutable terminal state.
     */
    public boolean isTerminal() {
        return this == DECLINED || this == EXPIRED || this == CANCELLED
                || this == CONFIRMED_NO_SHOW || this == MEDICAL_REJECTION || this == CANCELLED_SAFE;
    }

    /**
     * Validates whether a state transition from the current status to {@code next} is allowed.
     */
    public boolean canTransitionTo(MatchStatus next) {
        if (next == null) {
            return false;
        }
        if (this == MATCHED) {
            return next == ACCEPTED || next == DECLINED || next == EXPIRED || next == CANCELLED;
        }
        if (this == ACCEPTED) {
            return next == ARRIVED || next == CANCELLED || next == CANCELLED_SAFE || next == CONFIRMED_NO_SHOW || next == EXPIRED;
        }
        if (this == ARRIVED) {
            return next == MEDICAL_REJECTION;
        }
        return false;
    }
}
