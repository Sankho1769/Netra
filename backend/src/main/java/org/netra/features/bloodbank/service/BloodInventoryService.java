package org.netra.features.bloodbank.service;

import org.netra.core.audit.AuditService;
import org.netra.core.exception.AccountStatusException;
import org.netra.core.exception.ResourceNotFoundException;
import org.netra.core.exception.UnauthorizedSessionAccessException;
import org.netra.core.exception.ValidationException;
import org.netra.core.security.SecurityUtils;
import org.netra.features.bloodbank.dto.BloodInventoryDto;
import org.netra.features.bloodbank.dto.UpdateInventoryRequest;
import org.netra.features.bloodbank.entity.BloodBank;
import org.netra.features.bloodbank.entity.BloodBankVerificationStatus;
import org.netra.features.bloodbank.entity.BloodInventory;
import org.netra.features.bloodbank.entity.InventoryFreshness;
import org.netra.features.bloodbank.repository.BloodBankRepository;
import org.netra.features.bloodbank.repository.BloodInventoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BloodInventoryService {

    private static final Logger log = LoggerFactory.getLogger(BloodInventoryService.class);

    private final BloodBankRepository bloodBankRepository;
    private final BloodInventoryRepository bloodInventoryRepository;
    private final AuditService auditService;
    private final BloodBankAuthorizationService authorizationService;

    public BloodInventoryService(
            BloodBankRepository bloodBankRepository,
            BloodInventoryRepository bloodInventoryRepository,
            AuditService auditService,
            BloodBankAuthorizationService authorizationService) {
        this.bloodBankRepository = bloodBankRepository;
        this.bloodInventoryRepository = bloodInventoryRepository;
        this.auditService = auditService;
        this.authorizationService = authorizationService;
    }

    @Transactional(readOnly = true)
    public List<BloodInventoryDto> getInventory(UUID bloodBankId) {
        if (!bloodBankRepository.existsById(bloodBankId)) {
            throw new ResourceNotFoundException("Blood bank not found with id: " + bloodBankId);
        }

        Instant now = Instant.now();
        return bloodInventoryRepository.findByBloodBankId(bloodBankId).stream()
                .map(inv -> new BloodInventoryDto(
                        inv.getId(),
                        inv.getBloodBankId(),
                        inv.getBloodGroup(),
                        inv.getUnitsAvailable(),
                        InventoryFreshness.evaluate(inv.getLastUpdatedAt(), now),
                        inv.getLastUpdatedAt()
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public BloodInventoryDto updateInventory(
            UUID bloodBankId,
            UpdateInventoryRequest request,
            String clientIp,
            String userAgent) {

        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedSessionAccessException("Authentication is required to update inventory."));

        // Enforce Role + Resource Ownership: ADMIN or (BLOODBANK + ACTIVE account for this blood bank)
        authorizationService.verifyCanManageBloodBank(currentUserId, bloodBankId);

        BloodBank bank = bloodBankRepository.findById(bloodBankId)
                .orElseThrow(() -> new ResourceNotFoundException("Blood bank not found with id: " + bloodBankId));

        if (bank.getVerificationStatus() == BloodBankVerificationStatus.SUSPENDED
                || bank.getVerificationStatus() == BloodBankVerificationStatus.REJECTED) {
            throw new AccountStatusException("Suspended or rejected blood bank cannot perform inventory operations.");
        }

        if (request.getUnitsAvailable() == null || request.getUnitsAvailable() < 0) {
            throw new ValidationException("Blood inventory units cannot be negative.");
        }

        if (request.getBloodGroup() == null) {
            throw new ValidationException("Blood group is required.");
        }

        Instant now = Instant.now();

        Optional<BloodInventory> existingOpt = bloodInventoryRepository.findByBloodBankIdAndBloodGroup(
                bloodBankId, request.getBloodGroup());

        BloodInventory saved;
        if (existingOpt.isPresent()) {
            BloodInventory existing = existingOpt.get();
            existing.setUnitsAvailable(request.getUnitsAvailable());
            existing.setLastUpdatedAt(now);
            existing.setUpdatedAt(now);
            // Synchronously flush to trigger optimistic lock verification (@Version) immediately
            saved = bloodInventoryRepository.saveAndFlush(existing);
        } else {
            BloodInventory newInventory = new BloodInventory(
                    bloodBankId,
                    request.getBloodGroup(),
                    request.getUnitsAvailable()
            );
            // Synchronously flush to trigger unique constraint verification immediately
            saved = bloodInventoryRepository.saveAndFlush(newInventory);
        }

        // Only executed if saveAndFlush succeeded without conflict
        auditService.logAuthEvent(
                "INVENTORY_UPDATED",
                currentUserId,
                clientIp,
                userAgent,
                "{\"bloodBankId\":\"" + bloodBankId + "\",\"bloodGroup\":\"" + request.getBloodGroup().getCode() + "\",\"unitsAvailable\":" + saved.getUnitsAvailable() + "}"
        );

        return new BloodInventoryDto(
                saved.getId(),
                saved.getBloodBankId(),
                saved.getBloodGroup(),
                saved.getUnitsAvailable(),
                InventoryFreshness.evaluate(saved.getLastUpdatedAt(), now),
                saved.getLastUpdatedAt()
        );
    }
}
