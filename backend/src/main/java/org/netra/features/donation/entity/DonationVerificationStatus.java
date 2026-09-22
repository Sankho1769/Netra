package org.netra.features.donation.entity;

/**
 * State lifecycle for verified donations.
 *
 * Valid transitions:
 * PENDING_VERIFICATION -> VERIFIED
 * PENDING_VERIFICATION -> REJECTED
 * PENDING_VERIFICATION -> CANCELLED
 *
 * Terminal states (VERIFIED, REJECTED, CANCELLED) cannot transition further through normal workflows.
 * Admin corrections may adjust records under audited conditions.
 */
public enum DonationVerificationStatus {
    PENDING_VERIFICATION,
    VERIFIED,
    REJECTED,
    CANCELLED;

    public boolean isTerminal() {
        return this == VERIFIED || this == REJECTED || this == CANCELLED;
    }

    public boolean canTransitionTo(DonationVerificationStatus next) {
        if (next == null) {
            return false;
        }
        if (this == PENDING_VERIFICATION) {
            return next == VERIFIED || next == REJECTED || next == CANCELLED;
        }
        return false;
    }
}
