package org.netra.features.bloodbank.service;

import org.netra.core.audit.AuditService;
import org.netra.core.exception.DuplicateResourceException;
import org.netra.core.exception.ResourceNotFoundException;
import org.netra.core.exception.UnauthorizedSessionAccessException;
import org.netra.core.exception.ValidationException;
import org.netra.core.security.SecurityUtils;
import org.netra.features.bloodbank.dto.BloodBankAccountDto;
import org.netra.features.bloodbank.dto.BloodBankDetailDto;
import org.netra.features.bloodbank.dto.BloodBankSummaryDto;
import org.netra.features.bloodbank.dto.BloodInventoryDto;
import org.netra.features.bloodbank.dto.CreateBloodBankRequest;
import org.netra.features.bloodbank.dto.UpdateBloodBankRequest;
import org.netra.features.bloodbank.entity.BloodBank;
import org.netra.features.bloodbank.entity.BloodBankAccount;
import org.netra.features.bloodbank.entity.BloodBankAccountStatus;
import org.netra.features.bloodbank.entity.BloodBankOperatingStatus;
import org.netra.features.bloodbank.entity.BloodBankVerificationStatus;
import org.netra.features.bloodbank.entity.InventoryFreshness;
import org.netra.features.bloodbank.repository.BloodBankAccountRepository;
import org.netra.features.bloodbank.repository.BloodBankRepository;
import org.netra.features.bloodbank.repository.BloodInventoryRepository;
import org.netra.features.donor.entity.BloodGroup;
import org.netra.features.user.entity.User;
import org.netra.features.user.entity.UserRole;
import org.netra.features.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.AbstractMap;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class BloodBankService {

    private static final Logger log = LoggerFactory.getLogger(BloodBankService.class);
    private static final int MAX_PAGE_SIZE = 50;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final double MAX_RADIUS_KM = 100.0;
    private static final double MIN_RADIUS_KM = 0.1;

    private final BloodBankRepository bloodBankRepository;
    private final BloodInventoryRepository bloodInventoryRepository;
    private final BloodBankAccountRepository bloodBankAccountRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final BloodBankAuthorizationService authorizationService;

    public BloodBankService(
            BloodBankRepository bloodBankRepository,
            BloodInventoryRepository bloodInventoryRepository,
            BloodBankAccountRepository bloodBankAccountRepository,
            UserRepository userRepository,
            AuditService auditService,
            BloodBankAuthorizationService authorizationService) {
        this.bloodBankRepository = bloodBankRepository;
        this.bloodInventoryRepository = bloodInventoryRepository;
        this.bloodBankAccountRepository = bloodBankAccountRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.authorizationService = authorizationService;
    }

    @Transactional(readOnly = true)
    public Page<BloodBankSummaryDto> discoverBloodBanks(
            String city,
            BloodGroup bloodGroup,
            BloodBankOperatingStatus operatingStatus,
            Pageable pageable) {

        int pageSize = pageable.getPageSize();
        if (pageSize <= 0) {
            pageSize = DEFAULT_PAGE_SIZE;
        } else if (pageSize > MAX_PAGE_SIZE) {
            pageSize = MAX_PAGE_SIZE;
        }

        Pageable boundedPageable = PageRequest.of(pageable.getPageNumber(), pageSize, pageable.getSort());
        BloodBankVerificationStatus verifiedStatus = BloodBankVerificationStatus.VERIFIED;

        Page<BloodBank> banks;

        if (city != null && !city.isBlank()) {
            String trimmedCity = city.trim();
            if (bloodGroup != null) {
                banks = bloodBankRepository.findAvailableByCityAndBloodGroup(trimmedCity, bloodGroup, verifiedStatus, boundedPageable);
            } else if (operatingStatus != null) {
                banks = bloodBankRepository.findByCityIgnoreCaseAndOperatingStatusAndVerificationStatus(
                        trimmedCity, operatingStatus, verifiedStatus, boundedPageable);
            } else {
                banks = bloodBankRepository.findByCityIgnoreCaseAndVerificationStatus(trimmedCity, verifiedStatus, boundedPageable);
            }
        } else if (bloodGroup != null) {
            banks = bloodBankRepository.findAvailableByBloodGroup(bloodGroup, verifiedStatus, boundedPageable);
        } else if (operatingStatus != null) {
            banks = bloodBankRepository.findByOperatingStatusAndVerificationStatus(operatingStatus, verifiedStatus, boundedPageable);
        } else {
            banks = bloodBankRepository.findByVerificationStatus(verifiedStatus, boundedPageable);
        }

        return banks.map(bank -> mapToSummaryDto(bank, null));
    }

    @Transactional(readOnly = true)
    public List<BloodBankSummaryDto> findNearbyBloodBanks(double latitude, double longitude, double radiusKm) {
        if (latitude < -90.0 || latitude > 90.0) {
            throw new ValidationException("Latitude must be between -90.0 and 90.0 degrees.");
        }
        if (longitude < -180.0 || longitude > 180.0) {
            throw new ValidationException("Longitude must be between -180.0 and 180.0 degrees.");
        }
        if (radiusKm < MIN_RADIUS_KM || radiusKm > MAX_RADIUS_KM) {
            throw new ValidationException("Search radius must be between " + MIN_RADIUS_KM + " and " + MAX_RADIUS_KM + " km.");
        }

        double deltaLat = radiusKm / 111.0;
        double cosLat = Math.cos(Math.toRadians(latitude));
        double deltaLng = cosLat > 0.0001 ? radiusKm / (111.0 * cosLat) : radiusKm / 111.0;

        double minLat = latitude - deltaLat;
        double maxLat = latitude + deltaLat;
        double minLng = longitude - deltaLng;
        double maxLng = longitude + deltaLng;

        List<BloodBank> candidates = bloodBankRepository.findNearbyCandidates(
                minLat, maxLat, minLng, maxLng, BloodBankVerificationStatus.VERIFIED);

        return candidates.stream()
                .map(bank -> {
                    double dist = calculateHaversineDistanceKm(latitude, longitude, bank.getLatitude(), bank.getLongitude());
                    return new AbstractMap.SimpleEntry<>(bank, dist);
                })
                .filter(entry -> entry.getValue() <= radiusKm)
                .sorted(Comparator.comparingDouble(Map.Entry::getValue))
                .map(entry -> mapToSummaryDto(entry.getKey(), Math.round(entry.getValue() * 10.0) / 10.0))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public BloodBankDetailDto getBloodBankById(UUID id, Double userLat, Double userLon) {
        BloodBank bank = bloodBankRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blood bank not found with id: " + id));

        Double distanceKm = null;
        if (userLat != null && userLon != null
                && userLat >= -90.0 && userLat <= 90.0
                && userLon >= -180.0 && userLon <= 180.0) {
            double rawDistance = calculateHaversineDistanceKm(userLat, userLon, bank.getLatitude(), bank.getLongitude());
            distanceKm = Math.round(rawDistance * 10.0) / 10.0;
        }

        Instant now = Instant.now();
        List<BloodInventoryDto> inventory = bloodInventoryRepository.findByBloodBankId(id).stream()
                .map(inv -> new BloodInventoryDto(
                        inv.getId(),
                        inv.getBloodBankId(),
                        inv.getBloodGroup(),
                        inv.getUnitsAvailable(),
                        InventoryFreshness.evaluate(inv.getLastUpdatedAt(), now),
                        inv.getLastUpdatedAt()
                ))
                .collect(Collectors.toList());

        BloodBankDetailDto dto = mapToDetailDto(bank, distanceKm);
        dto.setInventory(inventory);
        return dto;
    }

    @Transactional
    public BloodBankDetailDto createBloodBank(CreateBloodBankRequest request, String clientIp, String userAgent) {
        UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);

        BloodBank bank = new BloodBank();
        bank.setName(request.getName().trim());
        bank.setRegistrationNumber(request.getRegistrationNumber() != null ? request.getRegistrationNumber().trim() : null);
        bank.setAddress(request.getAddress().trim());
        bank.setCity(request.getCity().trim());
        bank.setState(request.getState().trim());
        bank.setPostalCode(request.getPostalCode().trim());
        bank.setLatitude(request.getLatitude());
        bank.setLongitude(request.getLongitude());
        bank.setPhone(request.getPhone().trim());
        bank.setEmail(request.getEmail() != null ? request.getEmail().trim() : null);
        bank.setOperatingStatus(request.getOperatingStatus() != null ? request.getOperatingStatus() : BloodBankOperatingStatus.OPEN);
        
        // CRITICAL: Newly created blood bank unconditionally starts as PENDING. Client cannot self-verify.
        bank.setVerificationStatus(BloodBankVerificationStatus.PENDING);
        bank.setCreatedAt(Instant.now());
        bank.setUpdatedAt(Instant.now());

        BloodBank saved = bloodBankRepository.save(bank);

        auditService.logAuthEvent(
                "BLOOD_BANK_CREATED",
                currentUserId,
                clientIp,
                userAgent,
                "{\"bloodBankId\":\"" + saved.getId() + "\",\"name\":\"" + saved.getName() + "\"}"
        );

        return mapToDetailDto(saved, null);
    }

    @Transactional
    public BloodBankDetailDto updateBloodBank(UUID id, UpdateBloodBankRequest request, String clientIp, String userAgent) {
        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedSessionAccessException("Authentication is required to update blood bank."));

        // Enforce server-side ownership rule: ADMIN or (BLOODBANK + ACTIVE account for this blood bank)
        authorizationService.verifyCanManageBloodBank(currentUserId, id);

        BloodBank bank = bloodBankRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blood bank not found with id: " + id));

        if (request.getName() != null && !request.getName().isBlank()) {
            bank.setName(request.getName().trim());
        }
        if (request.getRegistrationNumber() != null) {
            bank.setRegistrationNumber(request.getRegistrationNumber().trim());
        }
        if (request.getAddress() != null && !request.getAddress().isBlank()) {
            bank.setAddress(request.getAddress().trim());
        }
        if (request.getCity() != null && !request.getCity().isBlank()) {
            bank.setCity(request.getCity().trim());
        }
        if (request.getState() != null && !request.getState().isBlank()) {
            bank.setState(request.getState().trim());
        }
        if (request.getPostalCode() != null && !request.getPostalCode().isBlank()) {
            bank.setPostalCode(request.getPostalCode().trim());
        }
        if (request.getLatitude() != null) {
            bank.setLatitude(request.getLatitude());
        }
        if (request.getLongitude() != null) {
            bank.setLongitude(request.getLongitude());
        }
        if (request.getPhone() != null && !request.getPhone().isBlank()) {
            bank.setPhone(request.getPhone().trim());
        }
        if (request.getEmail() != null) {
            bank.setEmail(request.getEmail().trim());
        }
        bank.setUpdatedAt(Instant.now());

        BloodBank updated = bloodBankRepository.save(bank);

        auditService.logAuthEvent(
                "BLOOD_BANK_UPDATED",
                currentUserId,
                clientIp,
                userAgent,
                "{\"bloodBankId\":\"" + updated.getId() + "\"}"
        );

        return mapToDetailDto(updated, null);
    }

    /**
     * Administrative transition for blood bank verification status.
     * Enforces strict workflow:
     * - PENDING -> VERIFIED or REJECTED
     * - VERIFIED -> SUSPENDED
     * - SUSPENDED -> VERIFIED or REJECTED
     * - REJECTED -> Terminal state
     */
    @Transactional
    public BloodBankDetailDto transitionVerificationStatus(
            UUID id,
            BloodBankVerificationStatus targetStatus,
            String clientIp,
            String userAgent) {

        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedSessionAccessException("Authentication is required."));

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedSessionAccessException("User not found."));

        if (!user.getRoles().contains(UserRole.ROLE_ADMIN)) {
            throw new UnauthorizedSessionAccessException("Only administrators can transition blood bank verification status.");
        }

        if (targetStatus == null) {
            throw new ValidationException("Target verification status is required.");
        }

        BloodBank bank = bloodBankRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blood bank not found with id: " + id));

        BloodBankVerificationStatus currentStatus = bank.getVerificationStatus();

        if (currentStatus == targetStatus) {
            throw new ValidationException("Blood bank is already in " + targetStatus + " status.");
        }

        boolean validTransition = false;
        switch (currentStatus) {
            case PENDING:
                validTransition = (targetStatus == BloodBankVerificationStatus.VERIFIED
                        || targetStatus == BloodBankVerificationStatus.REJECTED);
                break;
            case VERIFIED:
                validTransition = (targetStatus == BloodBankVerificationStatus.SUSPENDED);
                break;
            case SUSPENDED:
                validTransition = (targetStatus == BloodBankVerificationStatus.VERIFIED
                        || targetStatus == BloodBankVerificationStatus.REJECTED);
                break;
            case REJECTED:
                validTransition = false;
                break;
        }

        if (!validTransition) {
            throw new ValidationException("Invalid verification status transition from " + currentStatus + " to " + targetStatus + ".");
        }

        bank.setVerificationStatus(targetStatus);
        bank.setUpdatedAt(Instant.now());

        BloodBank updated = bloodBankRepository.save(bank);

        String eventType = "BLOOD_BANK_VERIFICATION_CHANGED";
        if (targetStatus == BloodBankVerificationStatus.VERIFIED) {
            eventType = "BLOOD_BANK_VERIFIED";
        } else if (targetStatus == BloodBankVerificationStatus.SUSPENDED) {
            eventType = "BLOOD_BANK_SUSPENDED";
        } else if (targetStatus == BloodBankVerificationStatus.REJECTED) {
            eventType = "BLOOD_BANK_REJECTED";
        }

        auditService.logAuthEvent(
                eventType,
                currentUserId,
                clientIp,
                userAgent,
                "{\"bloodBankId\":\"" + updated.getId() + "\",\"fromStatus\":\"" + currentStatus + "\",\"toStatus\":\"" + targetStatus + "\"}"
        );

        return mapToDetailDto(updated, null);
    }

    /**
     * Updates operating status (OPEN, CLOSED, TEMPORARILY_UNAVAILABLE).
     * Accessible by ADMIN or authorized staff linked to this blood bank.
     */
    @Transactional
    public BloodBankDetailDto updateOperatingStatus(
            UUID id,
            BloodBankOperatingStatus operatingStatus,
            String clientIp,
            String userAgent) {

        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedSessionAccessException("Authentication is required."));

        authorizationService.verifyCanManageBloodBank(currentUserId, id);

        if (operatingStatus == null) {
            throw new ValidationException("Operating status is required.");
        }

        BloodBank bank = bloodBankRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blood bank not found with id: " + id));

        bank.setOperatingStatus(operatingStatus);
        bank.setUpdatedAt(Instant.now());

        BloodBank updated = bloodBankRepository.save(bank);

        auditService.logAuthEvent(
                "BLOOD_BANK_STATUS_CHANGED",
                currentUserId,
                clientIp,
                userAgent,
                "{\"bloodBankId\":\"" + updated.getId() + "\",\"operatingStatus\":\"" + operatingStatus + "\"}"
        );

        return mapToDetailDto(updated, null);
    }

    /**
     * Links a user (ROLE_BLOODBANK) to a blood bank with an ACTIVE BloodBankAccount.
     * Admin only.
     */
    @Transactional
    public BloodBankAccountDto linkStaffAccount(
            UUID bloodBankId,
            UUID targetUserId,
            String clientIp,
            String userAgent) {

        UUID adminId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedSessionAccessException("Authentication is required."));

        User adminUser = userRepository.findById(adminId)
                .orElseThrow(() -> new UnauthorizedSessionAccessException("User not found."));

        if (!adminUser.getRoles().contains(UserRole.ROLE_ADMIN)) {
            throw new UnauthorizedSessionAccessException("Only administrators can link staff accounts.");
        }

        if (!bloodBankRepository.existsById(bloodBankId)) {
            throw new ResourceNotFoundException("Blood bank not found with id: " + bloodBankId);
        }

        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + targetUserId));

        if (!targetUser.getRoles().contains(UserRole.ROLE_BLOODBANK)) {
            targetUser.getRoles().add(UserRole.ROLE_BLOODBANK);
            userRepository.save(targetUser);
        }

        Optional<BloodBankAccount> existingOpt = bloodBankAccountRepository.findByUserIdAndBloodBankId(targetUserId, bloodBankId);

        BloodBankAccount account;
        if (existingOpt.isPresent()) {
            account = existingOpt.get();
            if (account.getStatus() == BloodBankAccountStatus.ACTIVE) {
                throw new DuplicateResourceException("User is already actively linked to this blood bank.");
            }
            account.setStatus(BloodBankAccountStatus.ACTIVE);
            account.setUpdatedAt(Instant.now());
            account = bloodBankAccountRepository.save(account);
        } else {
            account = new BloodBankAccount(targetUserId, bloodBankId, BloodBankAccountStatus.ACTIVE);
            account = bloodBankAccountRepository.save(account);
        }

        auditService.logAuthEvent(
                "ROLE_GRANTED",
                adminId,
                clientIp,
                userAgent,
                "{\"targetUserId\":\"" + targetUserId + "\",\"role\":\"ROLE_BLOODBANK\",\"bloodBankId\":\"" + bloodBankId + "\"}"
        );

        auditService.logAuthEvent(
                "BLOOD_BANK_ACCOUNT_CREATED",
                adminId,
                clientIp,
                userAgent,
                "{\"accountId\":\"" + account.getId() + "\",\"userId\":\"" + targetUserId + "\",\"bloodBankId\":\"" + bloodBankId + "\"}"
        );

        return mapToAccountDto(account);
    }

    /**
     * Updates staff account status (ACTIVE, SUSPENDED, REVOKED).
     * Admin only.
     */
    @Transactional
    public BloodBankAccountDto updateStaffAccountStatus(
            UUID bloodBankId,
            UUID accountId,
            BloodBankAccountStatus newStatus,
            String clientIp,
            String userAgent) {

        UUID adminId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedSessionAccessException("Authentication is required."));

        User adminUser = userRepository.findById(adminId)
                .orElseThrow(() -> new UnauthorizedSessionAccessException("User not found."));

        if (!adminUser.getRoles().contains(UserRole.ROLE_ADMIN)) {
            throw new UnauthorizedSessionAccessException("Only administrators can manage staff account status.");
        }

        BloodBankAccount account = bloodBankAccountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff account not found with id: " + accountId));

        if (!account.getBloodBankId().equals(bloodBankId)) {
            throw new ValidationException("Staff account does not belong to the specified blood bank.");
        }

        account.setStatus(newStatus);
        account.setUpdatedAt(Instant.now());

        BloodBankAccount saved = bloodBankAccountRepository.save(account);

        String eventType = "BLOOD_BANK_ACCOUNT_UPDATED";
        if (newStatus == BloodBankAccountStatus.SUSPENDED) {
            eventType = "BLOOD_BANK_ACCOUNT_SUSPENDED";
        } else if (newStatus == BloodBankAccountStatus.REVOKED) {
            eventType = "BLOOD_BANK_ACCOUNT_REVOKED";
        } else if (newStatus == BloodBankAccountStatus.ACTIVE) {
            eventType = "BLOOD_BANK_ACCOUNT_REACTIVATED";
        }

        auditService.logAuthEvent(
                eventType,
                adminId,
                clientIp,
                userAgent,
                "{\"accountId\":\"" + saved.getId() + "\",\"bloodBankId\":\"" + bloodBankId + "\",\"userId\":\"" + saved.getUserId() + "\",\"newStatus\":\"" + newStatus + "\"}"
        );

        return mapToAccountDto(saved);
    }

    public static double calculateHaversineDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        final int EARTH_RADIUS_KM = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    private BloodBankSummaryDto mapToSummaryDto(BloodBank bank, Double distanceKm) {
        return new BloodBankSummaryDto(
                bank.getId(),
                bank.getName(),
                bank.getRegistrationNumber(),
                bank.getAddress(),
                bank.getCity(),
                bank.getState(),
                bank.getPostalCode(),
                bank.getPhone(),
                bank.getEmail(),
                bank.getVerificationStatus(),
                bank.getOperatingStatus(),
                distanceKm
        );
    }

    private BloodBankDetailDto mapToDetailDto(BloodBank bank, Double distanceKm) {
        BloodBankDetailDto dto = new BloodBankDetailDto();
        dto.setId(bank.getId());
        dto.setName(bank.getName());
        dto.setRegistrationNumber(bank.getRegistrationNumber());
        dto.setAddress(bank.getAddress());
        dto.setCity(bank.getCity());
        dto.setState(bank.getState());
        dto.setPostalCode(bank.getPostalCode());
        dto.setLatitude(bank.getLatitude());
        dto.setLongitude(bank.getLongitude());
        dto.setPhone(bank.getPhone());
        dto.setEmail(bank.getEmail());
        dto.setVerificationStatus(bank.getVerificationStatus());
        dto.setOperatingStatus(bank.getOperatingStatus());
        dto.setDistanceKm(distanceKm);
        dto.setCreatedAt(bank.getCreatedAt());
        dto.setUpdatedAt(bank.getUpdatedAt());
        return dto;
    }

    private BloodBankAccountDto mapToAccountDto(BloodBankAccount account) {
        return new BloodBankAccountDto(
                account.getId(),
                account.getUserId(),
                account.getBloodBankId(),
                account.getStatus(),
                account.getCreatedAt(),
                account.getUpdatedAt()
        );
    }
}
