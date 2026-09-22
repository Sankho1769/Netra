package org.netra.features.donation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.netra.core.exception.ValidationException;
import org.netra.features.donation.entity.Donation;
import org.netra.features.donation.entity.DonationSourceType;
import org.netra.features.donation.entity.DonationVerificationStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DonationLifecycleTest {

    @Test
    @DisplayName("Lifecycle: PENDING_VERIFICATION can transition to VERIFIED, REJECTED, CANCELLED")
    void testValidTransitionsFromPending() {
        assertTrue(DonationVerificationStatus.PENDING_VERIFICATION.canTransitionTo(DonationVerificationStatus.VERIFIED));
        assertTrue(DonationVerificationStatus.PENDING_VERIFICATION.canTransitionTo(DonationVerificationStatus.REJECTED));
        assertTrue(DonationVerificationStatus.PENDING_VERIFICATION.canTransitionTo(DonationVerificationStatus.CANCELLED));
        assertFalse(DonationVerificationStatus.PENDING_VERIFICATION.canTransitionTo(DonationVerificationStatus.PENDING_VERIFICATION));
        assertFalse(DonationVerificationStatus.PENDING_VERIFICATION.canTransitionTo(null));
    }

    @Test
    @DisplayName("Lifecycle: Terminal states cannot transition to any other status")
    void testTerminalStatesCannotTransition() {
        assertTrue(DonationVerificationStatus.VERIFIED.isTerminal());
        assertTrue(DonationVerificationStatus.REJECTED.isTerminal());
        assertTrue(DonationVerificationStatus.CANCELLED.isTerminal());
        assertFalse(DonationVerificationStatus.PENDING_VERIFICATION.isTerminal());

        for (DonationVerificationStatus terminal : new DonationVerificationStatus[]{
                DonationVerificationStatus.VERIFIED,
                DonationVerificationStatus.REJECTED,
                DonationVerificationStatus.CANCELLED
        }) {
            for (DonationVerificationStatus target : DonationVerificationStatus.values()) {
                assertFalse(terminal.canTransitionTo(target),
                        terminal + " should not be able to transition to " + target);
            }
        }
    }

    @Test
    @DisplayName("Entity: Source reference validation enforces mutually exclusive references")
    void testSourceReferenceValidation() {
        UUID donorId = UUID.randomUUID();
        UUID reqId = UUID.randomUUID();
        UUID evtId = UUID.randomUUID();

        // Valid BLOOD_REQUEST
        assertDoesNotThrow(() -> new Donation(donorId, DonationSourceType.BLOOD_REQUEST, reqId, null, LocalDate.now(), null));

        // Valid DONATION_EVENT
        assertDoesNotThrow(() -> new Donation(donorId, DonationSourceType.DONATION_EVENT, null, evtId, LocalDate.now(), null));

        // Invalid: BLOOD_REQUEST with null reqId
        assertThrows(ValidationException.class, () ->
                new Donation(donorId, DonationSourceType.BLOOD_REQUEST, null, null, LocalDate.now(), null));

        // Invalid: BLOOD_REQUEST with both reqId and evtId
        assertThrows(ValidationException.class, () ->
                new Donation(donorId, DonationSourceType.BLOOD_REQUEST, reqId, evtId, LocalDate.now(), null));

        // Invalid: DONATION_EVENT with null evtId
        assertThrows(ValidationException.class, () ->
                new Donation(donorId, DonationSourceType.DONATION_EVENT, null, null, LocalDate.now(), null));

        // Invalid: DONATION_EVENT with both reqId and evtId
        assertThrows(ValidationException.class, () ->
                new Donation(donorId, DonationSourceType.DONATION_EVENT, reqId, evtId, LocalDate.now(), null));
    }

    @Test
    @DisplayName("Entity: verify() transitions status to VERIFIED and sets verifier and timestamp")
    void testVerifyMethod() {
        UUID donorId = UUID.randomUUID();
        UUID reqId = UUID.randomUUID();
        UUID verifierId = UUID.randomUUID();
        Instant now = Instant.now();

        Donation donation = new Donation(donorId, DonationSourceType.BLOOD_REQUEST, reqId, null, LocalDate.now(), "Notes");
        donation.verify(verifierId, now, "Verified at blood bank");

        assertEquals(DonationVerificationStatus.VERIFIED, donation.getVerificationStatus());
        assertEquals(verifierId, donation.getVerifiedByUserId());
        assertEquals(now, donation.getVerifiedAt());
        assertTrue(donation.getNotes().contains("Verified at blood bank"));

        // Second verification should throw ValidationException
        assertThrows(ValidationException.class, () -> donation.verify(verifierId, now, null));
    }

    @Test
    @DisplayName("Entity: reject() requires reason and transitions status to REJECTED")
    void testRejectMethod() {
        UUID donorId = UUID.randomUUID();
        UUID reqId = UUID.randomUUID();
        UUID verifierId = UUID.randomUUID();
        Instant now = Instant.now();

        Donation donation = new Donation(donorId, DonationSourceType.BLOOD_REQUEST, reqId, null, LocalDate.now(), null);

        // Reject with null or empty reason should fail
        assertThrows(ValidationException.class, () -> donation.reject(verifierId, now, null));
        assertThrows(ValidationException.class, () -> donation.reject(verifierId, now, "   "));

        donation.reject(verifierId, now, "Did not show up");
        assertEquals(DonationVerificationStatus.REJECTED, donation.getVerificationStatus());
        assertEquals("Did not show up", donation.getRejectionReason());
        assertEquals(verifierId, donation.getVerifiedByUserId());

        // Reject after already rejected fails
        assertThrows(ValidationException.class, () -> donation.reject(verifierId, now, "Already rejected"));
    }

    @Test
    @DisplayName("Entity: cancel() transitions status to CANCELLED")
    void testCancelMethod() {
        UUID donorId = UUID.randomUUID();
        UUID reqId = UUID.randomUUID();

        Donation donation = new Donation(donorId, DonationSourceType.BLOOD_REQUEST, reqId, null, LocalDate.now(), null);
        donation.cancel(Instant.now());

        assertEquals(DonationVerificationStatus.CANCELLED, donation.getVerificationStatus());

        // Verify or reject after cancel should fail
        assertThrows(ValidationException.class, () -> donation.verify(UUID.randomUUID(), Instant.now(), null));
        assertThrows(ValidationException.class, () -> donation.reject(UUID.randomUUID(), Instant.now(), "reason"));
    }
}
