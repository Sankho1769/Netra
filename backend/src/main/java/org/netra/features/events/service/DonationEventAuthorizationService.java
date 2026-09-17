package org.netra.features.events.service;

import org.netra.core.exception.AccountStatusException;
import org.netra.core.exception.ResourceNotFoundException;
import org.netra.core.exception.UnauthorizedSessionAccessException;
import org.netra.features.bloodbank.entity.BloodBank;
import org.netra.features.bloodbank.entity.BloodBankVerificationStatus;
import org.netra.features.bloodbank.repository.BloodBankRepository;
import org.netra.features.bloodbank.service.BloodBankAuthorizationService;
import org.netra.features.events.entity.DonationEvent;
import org.netra.features.user.entity.User;
import org.netra.features.user.entity.UserRole;
import org.netra.features.user.entity.UserStatus;
import org.netra.features.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class DonationEventAuthorizationService {

    private static final Logger log = LoggerFactory.getLogger(DonationEventAuthorizationService.class);

    private final UserRepository userRepository;
    private final BloodBankRepository bloodBankRepository;
    private final BloodBankAuthorizationService bloodBankAuthorizationService;

    public DonationEventAuthorizationService(
            UserRepository userRepository,
            BloodBankRepository bloodBankRepository,
            BloodBankAuthorizationService bloodBankAuthorizationService) {
        this.userRepository = userRepository;
        this.bloodBankRepository = bloodBankRepository;
        this.bloodBankAuthorizationService = bloodBankAuthorizationService;
    }

    public void verifyCanCreateEvent(UUID userId, UUID bloodBankId) {
        User user = getActiveUser(userId);

        BloodBank bank = bloodBankRepository.findById(bloodBankId)
                .orElseThrow(() -> new ResourceNotFoundException("Blood bank not found with id: " + bloodBankId));

        if (bank.getVerificationStatus() == BloodBankVerificationStatus.REJECTED) {
            throw new UnauthorizedSessionAccessException("Cannot create donation events for a rejected blood bank.");
        }

        bloodBankAuthorizationService.verifyCanManageBloodBank(userId, bloodBankId);
    }

    public boolean isAuthorizedToManageBank(UUID userId, UUID bloodBankId) {
        return bloodBankAuthorizationService.isAuthorizedToManage(userId, bloodBankId);
    }

    public void verifyCanManageBloodBank(UUID userId, UUID bloodBankId) {
        bloodBankAuthorizationService.verifyCanManageBloodBank(userId, bloodBankId);
    }

    public void verifyCanManageEvent(UUID userId, DonationEvent event) {
        getActiveUser(userId);
        if (event == null) {
            throw new ResourceNotFoundException("Donation event not found.");
        }
        bloodBankAuthorizationService.verifyCanManageBloodBank(userId, event.getBloodBankId());
    }

    public void verifyCanApproveEvent(UUID userId) {
        User user = getActiveUser(userId);
        if (!user.getRoles().contains(UserRole.ROLE_ADMIN)) {
            log.warn("Non-admin user {} attempted to review/approve donation event", userId);
            throw new UnauthorizedSessionAccessException("Only administrators can approve or reject donation events.");
        }
    }

    public void verifyCanViewAttendeeList(UUID userId, DonationEvent event) {
        getActiveUser(userId);
        if (event == null) {
            throw new ResourceNotFoundException("Donation event not found.");
        }
        bloodBankAuthorizationService.verifyCanManageBloodBank(userId, event.getBloodBankId());
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
