package org.netra.features.matching.entity;

/**
 * State lifecycle for persistent donor matches.
 *
 * Valid transitions:
 * MATCHED -> ACCEPTED
 * MATCHED -> DECLINED
 * MATCHED -> EXPIRED
 * MATCHED -> CANCELLED
 *
 * Terminal states (ACCEPTED, DECLINED, EXPIRED, CANCELLED) cannot transition to any status.
 */
public enum MatchStatus {
    MATCHED,
    ACCEPTED,
    DECLINED,
    EXPIRED,
    CANCELLED;

    /**
     * Determines whether the status represents a final, immutable terminal state.
     */
    public boolean isTerminal() {
        return this == ACCEPTED || this == DECLINED || this == EXPIRED || this == CANCELLED;
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
        return false;
    }
}
