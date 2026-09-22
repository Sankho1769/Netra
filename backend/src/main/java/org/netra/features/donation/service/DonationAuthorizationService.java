package org.netra.features.donation.service;

import org.netra.core.exception.AccountStatusException;
import org.netra.core.exception.UnauthorizedSessionAccessException;
import org.netra.features.bloodbank.entity.BloodBankAccountStatus;
import org.netra.features.bloodbank.repository.BloodBankAccountRepository;
import org.netra.features.donation.entity.Donation;
import org.netra.features.donation.entity.DonationSourceType;
import org.netra.features.events.entity.DonationEvent;
import org.netra.features.events.repository.DonationEventRepository;
import org.netra.features.user.entity.User;
import org.netra.features.user.entity.UserRole;
import org.netra.features.user.entity.UserStatus;
import org.netra.features.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Authorization service enforcing role, relationship, and resource ownership rules
 * for the verified donation workflow.
 *
 * Never trusts client-supplied verifier, donor, or organization IDs.
 */
@Service
public class DonationAuthorizationService {

    private static final Logger log = LoggerFactory.getLogger(DonationAuthorizationService.class);

    private final UserRepository userRepository;
    private final BloodBankAccountRepository bloodBankAccountRepository;
    private final DonationEventRepository donationEventRepository;

    public DonationAuthorizationService(
            UserRepository userRepository,
            BloodBankAccountRepository bloodBankAccountRepository,
            DonationEventRepository donationEventRepository) {
        this.userRepository = userRepository;
        this.bloodBankAccountRepository = bloodBankAccountRepository;
        this.donationEventRepository = donationEventRepository;
    }

    /**
     * Enforces that the user is authorized to verify or reject the given donation.
     *
     * Rules:
     * - User must be ACTIVE.
     * - ROLE_ADMIN is always authorized.
     * - For DONATION_EVENT:
     *     - ROLE_BLOODBANK: authorized if user has an ACTIVE BloodBankAccount linking to the event's blood bank.
     *     - ROLE_ORGANIZATION: authorized if user created the donation event.
     * - For BLOOD_REQUEST:
     *     - ROLE_BLOODBANK: authorized if user has an ACTIVE BloodBankAccount at an active blood bank.
     * - ROLE_DONOR and ROLE_RECEIVER are NEVER authorized to verify.
     */
    public void verifyCanVerifyDonation(UUID userId, Donation donation) {
        User user = getActiveUser(userId);

        if (user.getRoles().contains(UserRole.ROLE_ADMIN)) {
            return;
        }

        if (user.getRoles().contains(UserRole.ROLE_BLOODBANK)) {
            if (donation.getSourceType() == DonationSourceType.DONATION_EVENT) {
                DonationEvent event = donationEventRepository.findById(donation.getDonationEventId()).orElse(null);
                if (event != null && bloodBankAccountRepository.existsByUserIdAndBloodBankIdAndStatus(
                        userId, event.getBloodBankId(), BloodBankAccountStatus.ACTIVE)) {
                    return;
                }
                log.warn("User {} with ROLE_BLOODBANK attempted to verify event donation {} without active account for bank",
                        userId, donation.getId());
                throw new UnauthorizedSessionAccessException("User is not authorized to verify donations for this blood bank's event.");
            } else {
                // BLOOD_REQUEST: clinical verification requires active blood bank account
                boolean hasActiveBank = bloodBankAccountRepository.findByUserId(userId).stream()
                        .anyMatch(a -> a.getStatus() == BloodBankAccountStatus.ACTIVE);
                if (hasActiveBank) {
                    return;
                }
                log.warn("User {} with ROLE_BLOODBANK has no active blood bank accounts to verify request donation {}",
                        userId, donation.getId());
                throw new UnauthorizedSessionAccessException("User has no active blood bank association to verify blood donations.");
            }
        }

        if (user.getRoles().contains(UserRole.ROLE_ORGANIZATION)) {
            if (donation.getSourceType() == DonationSourceType.DONATION_EVENT) {
                DonationEvent event = donationEventRepository.findById(donation.getDonationEventId()).orElse(null);
                if (event != null && userId.equals(event.getCreatedBy())) {
                    return;
                }
            }
            log.warn("User {} with ROLE_ORGANIZATION attempted unauthorized donation verification for donation {}",
                    userId, donation.getId());
            throw new UnauthorizedSessionAccessException("Organization user is not authorized to verify this donation.");
        }

        log.warn("Security Alert: User {} with roles {} attempted to verify donation {}", userId, user.getRoles(), donation.getId());
        throw new UnauthorizedSessionAccessException("User role is not authorized to verify blood donations.");
    }

    /**
     * Enforces that the user is authorized to view the donation detail.
     */
    public void verifyCanViewDonation(UUID userId, Donation donation) {
        User user = getActiveUser(userId);

        // Donor who made the donation can view it
        if (donation.getDonorUserId().equals(userId)) {
            return;
        }

        // Verifiers (Admin, Blood Bank staff, Organization) can view
        if (user.getRoles().contains(UserRole.ROLE_ADMIN) ||
            user.getRoles().contains(UserRole.ROLE_BLOODBANK) ||
            user.getRoles().contains(UserRole.ROLE_ORGANIZATION)) {
            return;
        }

        throw new UnauthorizedSessionAccessException("You are not authorized to view this donation record.");
    }

    /**
     * Enforces that only the donor who claimed the donation can cancel the pending claim.
     */
    public void verifyCanCancelClaim(UUID userId, Donation donation) {
        getActiveUser(userId);
        if (!donation.getDonorUserId().equals(userId)) {
            throw new UnauthorizedSessionAccessException("Only the claiming donor can cancel this donation claim.");
        }
    }

    private User getActiveUser(UUID userId) {
        if (userId == null) {
            throw new UnauthorizedSessionAccessException("Authentication is required.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedSessionAccessException("User not found: " + userId));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AccountStatusException("User account is not active. Current status: " + user.getStatus());
        }
        return user;
    }
}
