package org.netra.features.fulfillment.service;

import org.netra.core.exception.AccountStatusException;
import org.netra.core.exception.UnauthorizedSessionAccessException;
import org.netra.features.bloodbank.entity.BloodBankAccountStatus;
import org.netra.features.bloodbank.repository.BloodBankAccountRepository;
import org.netra.features.bloodrequest.entity.BloodRequest;
import org.netra.features.donation.entity.Donation;
import org.netra.features.donation.entity.DonationSourceType;
import org.netra.features.events.entity.DonationEvent;
import org.netra.features.events.repository.DonationEventRepository;
import org.netra.features.fulfillment.entity.Fulfillment;
import org.netra.features.user.entity.User;
import org.netra.features.user.entity.UserRole;
import org.netra.features.user.entity.UserStatus;
import org.netra.features.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Authorization service enforcing RBAC and resource ownership rules
 * for the operational fulfillment workflow.
 *
 * Rule 8: Enforce existing RBAC and resource ownership rules.
 * Rule 9: Donor and receiver users cannot falsely complete fulfillment.
 * Scoping Rule: For any resource-level authorization involving a specific blood bank,
 * the authorization must remain strictly scoped to:
 *   userId + bloodBankId + account status
 * using the existing existsByUserIdAndBloodBankIdAndStatus(...).
 */
@Service
public class FulfillmentAuthorizationService {

    private static final Logger log = LoggerFactory.getLogger(FulfillmentAuthorizationService.class);

    private final UserRepository userRepository;
    private final BloodBankAccountRepository bloodBankAccountRepository;
    private final DonationEventRepository donationEventRepository;

    @Autowired
    public FulfillmentAuthorizationService(
            UserRepository userRepository,
            BloodBankAccountRepository bloodBankAccountRepository,
            DonationEventRepository donationEventRepository) {
        this.userRepository = userRepository;
        this.bloodBankAccountRepository = bloodBankAccountRepository;
        this.donationEventRepository = donationEventRepository;
    }

    public FulfillmentAuthorizationService(
            UserRepository userRepository,
            BloodBankAccountRepository bloodBankAccountRepository) {
        this(userRepository, bloodBankAccountRepository, null);
    }

    /**
     * Resolves the specific blood bank associated with a donation, if any.
     * If the donation originated from a donation event, the blood bank is the event's host blood bank.
     */
    public UUID resolveSpecificBloodBankId(Donation donation) {
        if (donation == null || donationEventRepository == null) {
            return null;
        }
        if (donation.getSourceType() == DonationSourceType.DONATION_EVENT && donation.getDonationEventId() != null) {
            DonationEvent event = donationEventRepository.findById(donation.getDonationEventId()).orElse(null);
            if (event != null) {
                return event.getBloodBankId();
            }
        }
        return null;
    }

    /**
     * Verifies whether a user with ROLE_BLOODBANK is authorized.
     * If a specific blood bank is involved, strictly validates:
     *   userId + bloodBankId + account status
     * via existsByUserIdAndBloodBankIdAndStatus.
     * Otherwise, checks whether the user has at least one active blood bank account.
     */
    private boolean isAuthorizedBloodBankStaff(UUID userId, UUID specificBloodBankId) {
        if (specificBloodBankId != null) {
            return bloodBankAccountRepository.existsByUserIdAndBloodBankIdAndStatus(
                    userId, specificBloodBankId, BloodBankAccountStatus.ACTIVE);
        }
        return bloodBankAccountRepository.findByUserId(userId).stream()
                .anyMatch(a -> a.getStatus() == BloodBankAccountStatus.ACTIVE);
    }

    /**
     * Enforces clinical authority required to operate fulfillments:
     * - Starting fulfillment (READY -> IN_PROGRESS)
     * - Completing fulfillment (IN_PROGRESS -> FULFILLED)
     * - Failing fulfillment (IN_PROGRESS -> FAILED)
     *
     * Only ROLE_BLOODBANK (with active account, scoped to event blood bank if applicable)
     * and ROLE_ADMIN possess clinical authority.
     * Donors and receivers are strictly forbidden.
     */
    public void verifyCanOperateFulfillment(UUID userId, Fulfillment fulfillment, Donation donation) {
        User user = getActiveUser(userId);

        if (user.getRoles().contains(UserRole.ROLE_ADMIN)) {
            return;
        }

        if (user.getRoles().contains(UserRole.ROLE_BLOODBANK)) {
            UUID specificBloodBankId = resolveSpecificBloodBankId(donation);
            boolean authorized = isAuthorizedBloodBankStaff(userId, specificBloodBankId);
            if (authorized) {
                return;
            }
            if (specificBloodBankId != null) {
                log.warn("User {} with ROLE_BLOODBANK attempted to operate fulfillment for blood bank {} without active account link",
                        userId, specificBloodBankId);
                throw new UnauthorizedSessionAccessException("User is not authorized to operate fulfillments for this blood bank's event donation.");
            }
            log.warn("User {} with ROLE_BLOODBANK attempted to operate fulfillment without an active blood bank account", userId);
            throw new UnauthorizedSessionAccessException("Active blood bank account is required to perform clinical fulfillment operations.");
        }

        log.warn("Unauthorized fulfillment operation attempt by user {} with roles {}", userId, user.getRoles());
        throw new UnauthorizedSessionAccessException("Only authorized blood bank staff or administrators can perform clinical fulfillment operations.");
    }

    public void verifyCanOperateFulfillment(UUID userId) {
        verifyCanOperateFulfillment(userId, null, null);
    }

    /**
     * Enforces authority to create a fulfillment claim.
     * Authorized: ROLE_ADMIN, active ROLE_BLOODBANK (scoped to donation's blood bank if event-based),
     * or the requester of the blood request.
     */
    public void verifyCanCreateFulfillment(UUID userId, BloodRequest bloodRequest, Donation donation) {
        User user = getActiveUser(userId);

        if (user.getRoles().contains(UserRole.ROLE_ADMIN)) {
            return;
        }

        if (user.getRoles().contains(UserRole.ROLE_BLOODBANK)) {
            UUID specificBloodBankId = resolveSpecificBloodBankId(donation);
            boolean authorized = isAuthorizedBloodBankStaff(userId, specificBloodBankId);
            if (authorized) {
                return;
            }
            if (specificBloodBankId != null) {
                log.warn("User {} with ROLE_BLOODBANK attempted to create fulfillment for blood bank {} without active account link",
                        userId, specificBloodBankId);
                throw new UnauthorizedSessionAccessException("User is not authorized to create fulfillments using this blood bank's event donation.");
            }
            throw new UnauthorizedSessionAccessException("Active blood bank account is required to create a fulfillment.");
        }

        if (bloodRequest.getRequesterUserId().equals(userId)) {
            return;
        }

        throw new UnauthorizedSessionAccessException("You are not authorized to create a fulfillment for this blood request.");
    }

    public void verifyCanCreateFulfillment(UUID userId, BloodRequest bloodRequest) {
        verifyCanCreateFulfillment(userId, bloodRequest, null);
    }

    /**
     * Enforces authority to cancel a fulfillment in READY status.
     * Authorized: Creator of the fulfillment, requester of the blood request,
     * active blood bank staff (scoped to blood bank if applicable), or admin.
     */
    public void verifyCanCancelFulfillment(UUID userId, Fulfillment fulfillment, BloodRequest bloodRequest, Donation donation) {
        User user = getActiveUser(userId);

        if (user.getRoles().contains(UserRole.ROLE_ADMIN)) {
            return;
        }

        if (fulfillment.getCreatedByUserId().equals(userId)) {
            return;
        }

        if (bloodRequest != null && bloodRequest.getRequesterUserId().equals(userId)) {
            return;
        }

        if (user.getRoles().contains(UserRole.ROLE_BLOODBANK)) {
            UUID specificBloodBankId = resolveSpecificBloodBankId(donation);
            if (isAuthorizedBloodBankStaff(userId, specificBloodBankId)) {
                return;
            }
        }

        throw new UnauthorizedSessionAccessException("You are not authorized to cancel this fulfillment.");
    }

    public void verifyCanCancelFulfillment(UUID userId, Fulfillment fulfillment, BloodRequest bloodRequest) {
        verifyCanCancelFulfillment(userId, fulfillment, bloodRequest, null);
    }

    /**
     * Enforces authority to view a fulfillment record.
     * Authorized: Creator, blood request requester, donation donor,
     * active blood bank staff (scoped to event blood bank if applicable), or admin.
     */
    public void verifyCanViewFulfillment(UUID userId, Fulfillment fulfillment, BloodRequest bloodRequest, Donation donation) {
        User user = getActiveUser(userId);

        if (user.getRoles().contains(UserRole.ROLE_ADMIN)) {
            return;
        }

        if (fulfillment.getCreatedByUserId().equals(userId)) {
            return;
        }

        if (bloodRequest != null && bloodRequest.getRequesterUserId().equals(userId)) {
            return;
        }

        if (donation != null && donation.getDonorUserId().equals(userId)) {
            return;
        }

        if (user.getRoles().contains(UserRole.ROLE_BLOODBANK)) {
            UUID specificBloodBankId = resolveSpecificBloodBankId(donation);
            if (isAuthorizedBloodBankStaff(userId, specificBloodBankId)) {
                return;
            }
        }

        throw new UnauthorizedSessionAccessException("You are not authorized to view this fulfillment record.");
    }

    /**
     * Enforces authority to view the pending fulfillments operational queue.
     * Authorized: ROLE_ADMIN, active ROLE_BLOODBANK.
     */
    public void verifyCanViewPendingQueue(UUID userId) {
        User user = getActiveUser(userId);

        if (user.getRoles().contains(UserRole.ROLE_ADMIN)) {
            return;
        }

        if (user.getRoles().contains(UserRole.ROLE_BLOODBANK)) {
            boolean hasActive = bloodBankAccountRepository.findByUserId(userId).stream()
                    .anyMatch(a -> a.getStatus() == BloodBankAccountStatus.ACTIVE);
            if (hasActive) {
                return;
            }
            log.warn("User {} with ROLE_BLOODBANK attempted to view pending queue without active blood bank account", userId);
            throw new UnauthorizedSessionAccessException("Active blood bank account is required to view the pending fulfillment queue.");
        }

        throw new UnauthorizedSessionAccessException("Only blood bank staff and administrators can view the pending fulfillment queue.");
    }

    public boolean canManage(UUID userId, User user) {
        if (user == null) {
            user = userRepository.findById(userId).orElse(null);
        }
        if (user == null) return false;
        return user.getRoles().contains(UserRole.ROLE_ADMIN) ||
                (user.getRoles().contains(UserRole.ROLE_BLOODBANK) &&
                        bloodBankAccountRepository.findByUserId(userId).stream()
                                .anyMatch(a -> a.getStatus() == BloodBankAccountStatus.ACTIVE));
    }

    private User getActiveUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedSessionAccessException("User not found: " + userId));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AccountStatusException("User account is " + user.getStatus() + ". Operation not permitted.");
        }
        return user;
    }
}
