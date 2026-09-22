package org.netra.features.notification.event;

import org.netra.features.notification.entity.NotificationReferenceType;
import org.netra.features.notification.entity.NotificationType;
import org.netra.features.notification.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

/**
 * Event listener that decouples domain operations from notification creation and delivery.
 * Listens for platform events AFTER_COMMIT so core transactions are never blocked or rolled back.
 */
@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final NotificationService notificationService;
    private final EmergencyNotificationHook emergencyNotificationHook;

    public NotificationEventListener(
            NotificationService notificationService,
            EmergencyNotificationHook emergencyNotificationHook) {
        this.notificationService = notificationService;
        this.emergencyNotificationHook = emergencyNotificationHook;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleMatchCreated(MatchCreatedEvent event) {
        try {
            notificationService.createNotification(
                    event.getDonorUserId(),
                    NotificationType.MATCH_CREATED,
                    "New Blood Donation Match Request",
                    "You have been matched with an urgent blood request. Please review details in the app to accept or decline.",
                    NotificationReferenceType.DONOR_MATCH,
                    event.getMatchId(),
                    "MATCH_CREATED:" + event.getMatchId()
            );
        } catch (Exception ex) {
            log.error("Error creating MATCH_CREATED notification for match {}: {}", event.getMatchId(), ex.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleMatchAccepted(MatchAcceptedEvent event) {
        try {
            notificationService.createNotification(
                    event.getRequesterUserId(),
                    NotificationType.MATCH_ACCEPTED,
                    "Donor Accepted Blood Request",
                    "A matched donor has accepted your blood request. Please review match details in the app.",
                    NotificationReferenceType.DONOR_MATCH,
                    event.getMatchId(),
                    "MATCH_ACCEPTED:" + event.getMatchId()
            );
        } catch (Exception ex) {
            log.error("Error creating MATCH_ACCEPTED notification for match {}: {}", event.getMatchId(), ex.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleMatchDeclined(MatchDeclinedEvent event) {
        try {
            notificationService.createNotification(
                    event.getRequesterUserId(),
                    NotificationType.MATCH_DECLINED,
                    "Donor Response Update",
                    "A matched donor was unable to accept your blood request.",
                    NotificationReferenceType.DONOR_MATCH,
                    event.getMatchId(),
                    "MATCH_DECLINED:" + event.getMatchId()
            );
        } catch (Exception ex) {
            log.error("Error creating MATCH_DECLINED notification for match {}: {}", event.getMatchId(), ex.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleMatchExpired(MatchExpiredEvent event) {
        try {
            notificationService.createNotification(
                    event.getDonorUserId(),
                    NotificationType.MATCH_EXPIRED,
                    "Match Request Expired",
                    "A blood donation match request has expired.",
                    NotificationReferenceType.DONOR_MATCH,
                    event.getMatchId(),
                    "MATCH_EXPIRED:" + event.getMatchId()
            );
        } catch (Exception ex) {
            log.error("Error creating MATCH_EXPIRED notification for match {}: {}", event.getMatchId(), ex.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleBloodRequestCancelled(BloodRequestCancelledEvent event) {
        for (UUID donorUserId : event.getAffectedDonorUserIds()) {
            try {
                notificationService.createNotification(
                        donorUserId,
                        NotificationType.BLOOD_REQUEST_CANCELLED,
                        "Blood Request Cancelled",
                        "A blood request you were matched with has been cancelled by the requester.",
                        NotificationReferenceType.BLOOD_REQUEST,
                        event.getBloodRequestId(),
                        "REQUEST_CANCELLED:" + event.getBloodRequestId() + ":" + donorUserId
                );
            } catch (Exception ex) {
                log.error("Error creating BLOOD_REQUEST_CANCELLED notification for donor {}: {}", donorUserId, ex.getMessage());
            }
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleEmergencyRequestCreated(EmergencyRequestCreatedEvent event) {
        try {
            emergencyNotificationHook.onEmergencyRequestCreated(event);

            // Notify the requester of successful emergency activation
            notificationService.createNotification(
                    event.getRequesterUserId(),
                    NotificationType.EMERGENCY_REQUEST_CREATED,
                    "Emergency Blood Request Activated",
                    "Your emergency blood request has been activated in the system. Eligible donors in your area are being matched.",
                    NotificationReferenceType.BLOOD_REQUEST,
                    event.getBloodRequestId(),
                    "EMERGENCY_CREATED:" + event.getBloodRequestId()
            );
        } catch (Exception ex) {
            log.error("Error handling EMERGENCY_REQUEST_CREATED event for request {}: {}", event.getBloodRequestId(), ex.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleDonationSubmitted(org.netra.features.donation.event.DonationSubmittedEvent event) {
        try {
            notificationService.createNotification(
                    event.getDonorUserId(),
                    NotificationType.DONATION_SUBMITTED,
                    "Donation Claim Submitted",
                    "Your blood donation claim for " + event.getDonationDate() + " has been submitted for verification.",
                    NotificationReferenceType.DONATION,
                    event.getDonationId(),
                    "DONATION_SUBMITTED:" + event.getDonationId()
            );
        } catch (Exception ex) {
            log.error("Error handling DONATION_SUBMITTED event for donation {}: {}", event.getDonationId(), ex.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleDonationVerified(org.netra.features.donation.event.DonationVerifiedEvent event) {
        try {
            notificationService.createNotification(
                    event.getDonorUserId(),
                    NotificationType.DONATION_VERIFIED,
                    "Blood Donation Verified",
                    "Your blood donation on " + event.getDonationDate() + " has been officially verified. Thank you for saving lives!",
                    NotificationReferenceType.DONATION,
                    event.getDonationId(),
                    "DONATION_VERIFIED:" + event.getDonationId()
            );
        } catch (Exception ex) {
            log.error("Error handling DONATION_VERIFIED event for donation {}: {}", event.getDonationId(), ex.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void handleDonationRejected(org.netra.features.donation.event.DonationRejectedEvent event) {
        try {
            notificationService.createNotification(
                    event.getDonorUserId(),
                    NotificationType.DONATION_REJECTED,
                    "Donation Verification Update",
                    "Your donation claim could not be verified: " + event.getRejectionReason(),
                    NotificationReferenceType.DONATION,
                    event.getDonationId(),
                    "DONATION_REJECTED:" + event.getDonationId()
            );
        } catch (Exception ex) {
            log.error("Error handling DONATION_REJECTED event for donation {}: {}", event.getDonationId(), ex.getMessage());
        }
    }
}
