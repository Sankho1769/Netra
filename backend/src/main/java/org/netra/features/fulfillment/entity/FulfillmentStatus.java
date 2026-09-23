package org.netra.features.fulfillment.entity;

/**
 * Lifecycle status for the Fulfillment aggregate.
 *
 * State Machine:
 * READY -> IN_PROGRESS -> FULFILLED
 * READY -> CANCELLED
 * IN_PROGRESS -> FAILED
 */
public enum FulfillmentStatus {
    READY,
    IN_PROGRESS,
    FULFILLED,
    CANCELLED,
    FAILED;

    public boolean canTransitionTo(FulfillmentStatus target) {
        if (target == null) {
            return false;
        }
        return switch (this) {
            case READY -> target == IN_PROGRESS || target == CANCELLED;
            case IN_PROGRESS -> target == FULFILLED || target == FAILED;
            case FULFILLED, CANCELLED, FAILED -> false;
        };
    }

    public boolean isTerminal() {
        return this == FULFILLED || this == CANCELLED || this == FAILED;
    }

    public boolean isActive() {
        return this == READY || this == IN_PROGRESS;
    }
}
