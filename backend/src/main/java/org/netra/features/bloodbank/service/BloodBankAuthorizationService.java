package org.netra.features.bloodbank.service;

import org.netra.core.exception.AccountStatusException;
import org.netra.core.exception.UnauthorizedSessionAccessException;
import org.netra.features.bloodbank.entity.BloodBankAccountStatus;
import org.netra.features.bloodbank.repository.BloodBankAccountRepository;
import org.netra.features.user.entity.User;
import org.netra.features.user.entity.UserRole;
import org.netra.features.user.entity.UserStatus;
import org.netra.features.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class BloodBankAuthorizationService {

    private static final Logger log = LoggerFactory.getLogger(BloodBankAuthorizationService.class);

    private final UserRepository userRepository;
    private final BloodBankAccountRepository bloodBankAccountRepository;

    public BloodBankAuthorizationService(
            UserRepository userRepository,
            BloodBankAccountRepository bloodBankAccountRepository) {
        this.userRepository = userRepository;
        this.bloodBankAccountRepository = bloodBankAccountRepository;
    }

    /**
     * Verifies whether an authenticated user is authorized to manage the specified blood bank.
     * Rule:
     * - User must exist and have UserStatus.ACTIVE.
     * - User with ROLE_ADMIN is authorized.
     * - User with ROLE_BLOODBANK is authorized ONLY IF an ACTIVE BloodBankAccount links them to the requested bloodBankId.
     * - DONOR and RECEIVER roles are NEVER authorized.
     */
    public boolean isAuthorizedToManage(UUID userId, UUID bloodBankId) {
        if (userId == null || bloodBankId == null) {
            return false;
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getStatus() != UserStatus.ACTIVE) {
            return false;
        }

        if (user.getRoles().contains(UserRole.ROLE_ADMIN)) {
            return true;
        }

        if (user.getRoles().contains(UserRole.ROLE_BLOODBANK)) {
            return bloodBankAccountRepository.existsByUserIdAndBloodBankIdAndStatus(
                    userId, bloodBankId, BloodBankAccountStatus.ACTIVE);
        }

        return false;
    }

    /**
     * Enforces that the user is authorized to manage the requested blood bank.
     * Throws UnauthorizedSessionAccessException (HTTP 403) or AccountStatusException if unauthorized.
     */
    public void verifyCanManageBloodBank(UUID userId, UUID bloodBankId) {
        if (userId == null) {
            throw new UnauthorizedSessionAccessException("Authentication is required to perform this action.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedSessionAccessException("User not found."));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AccountStatusException("User account is not active. Status: " + user.getStatus());
        }

        if (user.getRoles().contains(UserRole.ROLE_ADMIN)) {
            return;
        }

        if (!user.getRoles().contains(UserRole.ROLE_BLOODBANK)) {
            log.warn("User {} attempted to manage blood bank {} without ROLE_BLOODBANK or ROLE_ADMIN", userId, bloodBankId);
            throw new UnauthorizedSessionAccessException("User role is not authorized to manage blood banks.");
        }

        boolean hasActiveAccount = bloodBankAccountRepository.existsByUserIdAndBloodBankIdAndStatus(
                userId, bloodBankId, BloodBankAccountStatus.ACTIVE);

        if (!hasActiveAccount) {
            log.warn("Security Alert - BOLA/IDOR attempt: User {} attempted to access blood bank {} without an ACTIVE BloodBankAccount link",
                    userId, bloodBankId);
            throw new UnauthorizedSessionAccessException("User is not authorized to manage this blood bank.");
        }
    }
}
