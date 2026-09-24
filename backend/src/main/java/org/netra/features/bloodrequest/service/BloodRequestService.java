package org.netra.features.bloodrequest.service;

import org.netra.core.audit.AuditService;
import org.netra.core.exception.ResourceNotFoundException;
import org.netra.core.exception.UnauthorizedSessionAccessException;
import org.netra.core.exception.ValidationException;
import org.netra.core.security.SecurityUtils;
import org.netra.features.bloodbank.service.BloodBankService;
import org.netra.features.bloodrequest.dto.*;
import org.netra.features.bloodrequest.entity.BloodRequest;
import org.netra.features.bloodrequest.entity.BloodRequestStatus;
import org.netra.features.bloodrequest.entity.BloodRequestUrgency;
import org.netra.features.bloodrequest.repository.BloodRequestRepository;
import org.netra.features.donor.entity.BloodGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BloodRequestService {

    private static final Logger log = LoggerFactory.getLogger(BloodRequestService.class);
    private static final double MIN_RADIUS_KM = 1.0;
    private static final double MAX_RADIUS_KM = 100.0;
    private static final int MAX_PAGE_SIZE = 50;

    private final BloodRequestRepository bloodRequestRepository;
    private final BloodRequestAuthorizationService authorizationService;
    private final AuditService auditService;
    private final org.netra.features.matching.service.DonorMatchLifecycleService donorMatchLifecycleService;
    private final org.netra.core.observability.NetraMetrics netraMetrics;

    public BloodRequestService(
            BloodRequestRepository bloodRequestRepository,
            BloodRequestAuthorizationService authorizationService,
            AuditService auditService) {
        this(bloodRequestRepository, authorizationService, auditService, null, null);
    }

    @org.springframework.beans.factory.annotation.Autowired
    public BloodRequestService(
            BloodRequestRepository bloodRequestRepository,
            BloodRequestAuthorizationService authorizationService,
            AuditService auditService,
            org.netra.features.matching.service.DonorMatchLifecycleService donorMatchLifecycleService,
            @org.springframework.beans.factory.annotation.Autowired(required = false)
            org.netra.core.observability.NetraMetrics netraMetrics) {
        this.bloodRequestRepository = bloodRequestRepository;
        this.authorizationService = authorizationService;
        this.auditService = auditService;
        this.donorMatchLifecycleService = donorMatchLifecycleService;
        this.netraMetrics = netraMetrics;
    }

    @Transactional
    public BloodRequestDetailDto createRequest(CreateBloodRequestRequest request, String clientIp, String userAgent) {
        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedSessionAccessException("Authentication is required to create a blood request."));

        authorizationService.verifyActiveUser(currentUserId);

        validateCreation(request);

        if (request.getUrgency() == BloodRequestUrgency.CRITICAL) {
            throw new ValidationException("Critical urgency requests must be created through Emergency Mode.");
        }

        return persistAndLogRequest(request, currentUserId, clientIp, userAgent);
    }

    @Transactional
    public BloodRequestDetailDto createEmergencyBloodRequest(CreateBloodRequestRequest request, String clientIp, String userAgent) {
        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedSessionAccessException("Authentication is required to create an emergency blood request."));

        authorizationService.verifyActiveUser(currentUserId);

        validateCreation(request);

        if (request.getUrgency() != BloodRequestUrgency.CRITICAL) {
            throw new ValidationException("Emergency blood requests must have CRITICAL urgency.");
        }

        return persistAndLogRequest(request, currentUserId, clientIp, userAgent);
    }

    private BloodRequestDetailDto persistAndLogRequest(
            CreateBloodRequestRequest request, UUID currentUserId, String clientIp, String userAgent) {
        BloodRequest bloodRequest = new BloodRequest();
        bloodRequest.setRequesterUserId(currentUserId);
        bloodRequest.setBloodGroup(request.getBloodGroup());
        bloodRequest.setUnitsRequired(request.getUnitsRequired());
        bloodRequest.setUrgency(request.getUrgency());
        bloodRequest.setStatus(BloodRequestStatus.OPEN);
        bloodRequest.setHospitalName(request.getHospitalName().trim());
        bloodRequest.setHospitalAddress(request.getHospitalAddress().trim());
        bloodRequest.setCity(request.getCity().trim());
        bloodRequest.setState(request.getState().trim());
        bloodRequest.setPostalCode(request.getPostalCode().trim());
        bloodRequest.setLatitude(request.getLatitude());
        bloodRequest.setLongitude(request.getLongitude());
        bloodRequest.setRequiredBy(request.getRequiredBy());
        if (request.getDescription() != null && !request.getDescription().isBlank()) {
            bloodRequest.setDescription(request.getDescription().trim());
        }

        Instant now = Instant.now();
        bloodRequest.setCreatedAt(now);
        bloodRequest.setUpdatedAt(now);

        BloodRequest saved = bloodRequestRepository.save(bloodRequest);

        auditService.logAuthEvent(
                "BLOOD_REQUEST_CREATED",
                currentUserId,
                clientIp,
                userAgent,
                "{\"requestId\":\"" + saved.getId() + "\"}"
        );

        if (netraMetrics != null) {
            netraMetrics.incrementBloodRequestsCreated(saved.getUrgency().name(), saved.getBloodGroup().name());
            if (saved.getUrgency() == BloodRequestUrgency.CRITICAL) {
                netraMetrics.incrementEmergencyRequestsCreated();
            }
        }
        org.netra.core.observability.StructuredLogger.logOperation(
                "BLOOD_REQUEST_CREATED", currentUserId, null, "BloodRequest", saved.getId(), "CREATE", null, "SUCCESS");

        return mapToDetailDto(saved, true, true, null);
    }

    @Transactional(readOnly = true)
    public BloodRequestPublicDetailDto getRequestById(UUID id) {
        BloodRequest bloodRequest = bloodRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blood request not found: " + id));

        UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        boolean isOwner = currentUserId != null && bloodRequest.getRequesterUserId().equals(currentUserId);
        boolean isAdmin = SecurityUtils.hasRole("ROLE_ADMIN");
        boolean canManage = isOwner || isAdmin;

        if (!canManage) {
            if (bloodRequest.getStatus() != BloodRequestStatus.OPEN || !bloodRequest.getRequiredBy().isAfter(Instant.now())) {
                throw new ResourceNotFoundException("Blood request not found: " + id);
            }
            return mapToPublicDetailDto(bloodRequest, null);
        }

        return mapToDetailDto(bloodRequest, isOwner, canManage, null);
    }

    @Transactional(readOnly = true)
    public Page<BloodRequestSummaryDto> getMyRequests(Pageable pageable) {
        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedSessionAccessException("Authentication is required."));

        authorizationService.verifyActiveUser(currentUserId);

        Pageable bounded = boundPageable(pageable);
        Page<BloodRequest> requests = bloodRequestRepository.findByRequesterUserId(currentUserId, bounded);
        return requests.map(r -> mapToSummaryDto(r, null));
    }

    @Transactional(readOnly = true)
    public Page<BloodRequestSummaryDto> discoverRequests(
            BloodGroup bloodGroup, String city, BloodRequestUrgency urgency, Pageable pageable) {
        Pageable bounded = boundPageable(pageable);
        String trimmedCity = (city != null && !city.isBlank()) ? city.trim() : null;

        Instant now = Instant.now();
        // V1 active discovery strictly exposes non-expired OPEN requests
        Page<BloodRequest> requests = bloodRequestRepository.findDiscoverableRequests(
                BloodRequestStatus.OPEN, now, bloodGroup, urgency, trimmedCity, bounded);

        return requests.map(r -> mapToSummaryDto(r, null));
    }

    @Transactional(readOnly = true)
    public List<BloodRequestSummaryDto> findNearbyRequests(
            double latitude, double longitude, double radiusKm, BloodGroup bloodGroup) {
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

        Instant now = Instant.now();
        List<BloodRequest> candidates = bloodRequestRepository.findNearbyCandidates(
                minLat, maxLat, minLng, maxLng, BloodRequestStatus.OPEN, now, bloodGroup);

        return candidates.stream()
                .map(req -> {
                    double dist = BloodBankService.calculateHaversineDistanceKm(
                            latitude, longitude, req.getLatitude(), req.getLongitude());
                    return new AbstractMap.SimpleEntry<>(req, dist);
                })
                .filter(entry -> entry.getValue() <= radiusKm)
                .sorted(Comparator.comparingDouble(Map.Entry::getValue))
                .map(entry -> mapToSummaryDto(entry.getKey(), Math.round(entry.getValue() * 10.0) / 10.0))
                .collect(Collectors.toList());
    }

    @Transactional
    public BloodRequestDetailDto updateRequest(UUID id, UpdateBloodRequestRequest request, String clientIp, String userAgent) {
        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedSessionAccessException("Authentication is required to update a blood request."));

        BloodRequest bloodRequest = bloodRequestRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blood request not found: " + id));

        authorizationService.verifyCanManageRequest(currentUserId, bloodRequest);

        if (bloodRequest.getStatus() != BloodRequestStatus.OPEN) {
            throw new ValidationException("Cannot update a blood request that is " + bloodRequest.getStatus() + ".");
        }

        if (request.getUnitsRequired() != null) {
            if (request.getUnitsRequired() < 1 || request.getUnitsRequired() > 50) {
                throw new ValidationException("Units required must be between 1 and 50.");
            }
            bloodRequest.setUnitsRequired(request.getUnitsRequired());
        }

        if (request.getUrgency() != null) {
            if (bloodRequest.getUrgency() == BloodRequestUrgency.CRITICAL) {
                if (request.getUrgency() != BloodRequestUrgency.CRITICAL) {
                    throw new ValidationException("Emergency requests must remain CRITICAL.");
                }
            } else {
                if (request.getUrgency() == BloodRequestUrgency.CRITICAL) {
                    throw new ValidationException("Critical urgency requests must be created through Emergency Mode.");
                }
            }
            bloodRequest.setUrgency(request.getUrgency());
        }

        if (request.getHospitalName() != null && !request.getHospitalName().isBlank()) {
            bloodRequest.setHospitalName(request.getHospitalName().trim());
        }
        if (request.getHospitalAddress() != null && !request.getHospitalAddress().isBlank()) {
            bloodRequest.setHospitalAddress(request.getHospitalAddress().trim());
        }
        if (request.getCity() != null && !request.getCity().isBlank()) {
            bloodRequest.setCity(request.getCity().trim());
        }
        if (request.getState() != null && !request.getState().isBlank()) {
            bloodRequest.setState(request.getState().trim());
        }
        if (request.getPostalCode() != null && !request.getPostalCode().isBlank()) {
            bloodRequest.setPostalCode(request.getPostalCode().trim());
        }

        if (request.getLatitude() != null) {
            if (request.getLatitude() < -90.0 || request.getLatitude() > 90.0) {
                throw new ValidationException("Latitude must be between -90.0 and 90.0 degrees.");
            }
            bloodRequest.setLatitude(request.getLatitude());
        }
        if (request.getLongitude() != null) {
            if (request.getLongitude() < -180.0 || request.getLongitude() > 180.0) {
                throw new ValidationException("Longitude must be between -180.0 and 180.0 degrees.");
            }
            bloodRequest.setLongitude(request.getLongitude());
        }

        if (request.getRequiredBy() != null) {
            if (!request.getRequiredBy().isAfter(Instant.now())) {
                throw new ValidationException("Required-by deadline must be in the future.");
            }
            bloodRequest.setRequiredBy(request.getRequiredBy());
        }

        if (request.getDescription() != null) {
            bloodRequest.setDescription(request.getDescription().trim());
        }

        bloodRequest.setUpdatedAt(Instant.now());
        BloodRequest saved = bloodRequestRepository.save(bloodRequest);

        auditService.logAuthEvent(
                "BLOOD_REQUEST_UPDATED",
                currentUserId,
                clientIp,
                userAgent,
                "{\"requestId\":\"" + saved.getId() + "\"}"
        );

        boolean isOwner = bloodRequest.getRequesterUserId().equals(currentUserId);
        return mapToDetailDto(saved, isOwner, true, null);
    }

    @Transactional
    public BloodRequestDetailDto cancelRequest(UUID id, CancelBloodRequestRequest request, String clientIp, String userAgent) {
        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedSessionAccessException("Authentication is required to cancel a blood request."));

        BloodRequest bloodRequest = bloodRequestRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blood request not found: " + id));

        authorizationService.verifyCanManageRequest(currentUserId, bloodRequest);

        if (bloodRequest.getStatus().isTerminal()) {
            throw new ValidationException("Cannot cancel a blood request that is already " + bloodRequest.getStatus() + ".");
        }

        Instant now = Instant.now();
        bloodRequest.setStatus(BloodRequestStatus.CANCELLED);
        bloodRequest.setCancelledAt(now);
        bloodRequest.setCancelledBy(currentUserId);
        if (request != null && request.getReason() != null && !request.getReason().isBlank()) {
            bloodRequest.setCancellationReason(request.getReason().trim());
        }
        bloodRequest.setUpdatedAt(now);

        BloodRequest saved = bloodRequestRepository.save(bloodRequest);

        if (donorMatchLifecycleService != null) {
            donorMatchLifecycleService.cancelActiveMatchesForRequest(saved.getId(), currentUserId, clientIp, userAgent);
        }

        auditService.logAuthEvent(
                "BLOOD_REQUEST_CANCELLED",
                currentUserId,
                clientIp,
                userAgent,
                "{\"requestId\":\"" + saved.getId() + "\"}"
        );

        if (netraMetrics != null) {
            netraMetrics.incrementBloodRequestsCancelled();
        }
        org.netra.core.observability.StructuredLogger.logOperation(
                "BLOOD_REQUEST_CANCELLED", currentUserId, null, "BloodRequest", saved.getId(), "CANCEL", null, "SUCCESS");

        boolean isOwner = bloodRequest.getRequesterUserId().equals(currentUserId);
        return mapToDetailDto(saved, isOwner, true, null);
    }

    @Transactional
    public BloodRequestDetailDto cancelEmergencyRequest(UUID id, CancelBloodRequestRequest request, String clientIp, String userAgent) {
        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedSessionAccessException("Authentication is required to cancel an emergency blood request."));

        BloodRequest bloodRequest = bloodRequestRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Blood request not found: " + id));

        // 1. Verify owner/admin authorization FIRST to avoid leaking urgency information
        authorizationService.verifyCanManageRequest(currentUserId, bloodRequest);

        // 2. Enforce Emergency invariant: target request must have CRITICAL urgency
        if (bloodRequest.getUrgency() != BloodRequestUrgency.CRITICAL) {
            throw new ValidationException("Only emergency requests with CRITICAL urgency can be cancelled through the emergency endpoint.");
        }

        if (bloodRequest.getStatus().isTerminal()) {
            throw new ValidationException("Cannot cancel a blood request that is already " + bloodRequest.getStatus() + ".");
        }

        Instant now = Instant.now();
        bloodRequest.setStatus(BloodRequestStatus.CANCELLED);
        bloodRequest.setCancelledAt(now);
        bloodRequest.setCancelledBy(currentUserId);
        if (request != null && request.getReason() != null && !request.getReason().isBlank()) {
            bloodRequest.setCancellationReason(request.getReason().trim());
        }
        bloodRequest.setUpdatedAt(now);

        BloodRequest saved = bloodRequestRepository.save(bloodRequest);

        if (donorMatchLifecycleService != null) {
            donorMatchLifecycleService.cancelActiveMatchesForRequest(saved.getId(), currentUserId, clientIp, userAgent);
        }

        auditService.logAuthEvent(
                "BLOOD_REQUEST_CANCELLED",
                currentUserId,
                clientIp,
                userAgent,
                "{\"requestId\":\"" + saved.getId() + "\"}"
        );

        auditService.logAuthEvent(
                "EMERGENCY_REQUEST_CANCELLED",
                currentUserId,
                clientIp,
                userAgent,
                "{\"requestId\":\"" + saved.getId() + "\"}"
        );

        if (netraMetrics != null) {
            netraMetrics.incrementBloodRequestsCancelled();
        }
        org.netra.core.observability.StructuredLogger.logOperation(
                "EMERGENCY_REQUEST_CANCELLED", currentUserId, null, "BloodRequest", saved.getId(), "CANCEL", null, "SUCCESS");

        boolean isOwner = bloodRequest.getRequesterUserId().equals(currentUserId);
        return mapToDetailDto(saved, isOwner, true, null);
    }

    private void validateCreation(CreateBloodRequestRequest request) {
        if (request.getBloodGroup() == null) {
            throw new ValidationException("Blood group is required.");
        }
        if (request.getUnitsRequired() == null || request.getUnitsRequired() < 1 || request.getUnitsRequired() > 50) {
            throw new ValidationException("Units required must be between 1 and 50.");
        }
        if (request.getHospitalName() == null || request.getHospitalName().isBlank()) {
            throw new ValidationException("Hospital name is required.");
        }
        if (request.getHospitalAddress() == null || request.getHospitalAddress().isBlank()) {
            throw new ValidationException("Hospital address is required.");
        }
        if (request.getCity() == null || request.getCity().isBlank()) {
            throw new ValidationException("City is required.");
        }
        if (request.getState() == null || request.getState().isBlank()) {
            throw new ValidationException("State is required.");
        }
        if (request.getPostalCode() == null || request.getPostalCode().isBlank()) {
            throw new ValidationException("Postal code is required.");
        }
        if (request.getLatitude() == null || request.getLatitude() < -90.0 || request.getLatitude() > 90.0) {
            throw new ValidationException("Latitude must be between -90.0 and 90.0 degrees.");
        }
        if (request.getLongitude() == null || request.getLongitude() < -180.0 || request.getLongitude() > 180.0) {
            throw new ValidationException("Longitude must be between -180.0 and 180.0 degrees.");
        }
        if (request.getRequiredBy() == null || !request.getRequiredBy().isAfter(Instant.now())) {
            throw new ValidationException("Required-by deadline must be in the future.");
        }
    }

    private Pageable boundPageable(Pageable pageable) {
        int page = Math.max(0, pageable.getPageNumber());
        int size = Math.min(MAX_PAGE_SIZE, Math.max(1, pageable.getPageSize()));
        Sort sort = pageable.getSort().isSorted() ? pageable.getSort() : Sort.by(Sort.Direction.DESC, "createdAt");
        return PageRequest.of(page, size, sort);
    }

    private BloodRequestSummaryDto mapToSummaryDto(BloodRequest request, Double distanceKm) {
        return new BloodRequestSummaryDto(
                request.getId(),
                request.getBloodGroup(),
                request.getUnitsRequired(),
                request.getUrgency(),
                request.getStatus(),
                request.getHospitalName(),
                request.getCity(),
                request.getState(),
                request.getRequiredBy(),
                distanceKm,
                request.getCreatedAt()
        );
    }

    private BloodRequestPublicDetailDto mapToPublicDetailDto(BloodRequest request, Double distanceKm) {
        BloodRequestPublicDetailDto dto = new BloodRequestPublicDetailDto();
        dto.setId(request.getId());
        dto.setBloodGroup(request.getBloodGroup());
        dto.setUnitsRequired(request.getUnitsRequired());
        dto.setUrgency(request.getUrgency());
        dto.setStatus(request.getStatus());
        dto.setHospitalName(request.getHospitalName());
        dto.setHospitalAddress(request.getHospitalAddress());
        dto.setCity(request.getCity());
        dto.setState(request.getState());
        dto.setPostalCode(request.getPostalCode());
        dto.setRequiredBy(request.getRequiredBy());
        dto.setCreatedAt(request.getCreatedAt());
        dto.setDistanceKm(distanceKm);
        dto.setIsOwner(false);
        dto.setCanManage(false);
        return dto;
    }

    private BloodRequestDetailDto mapToDetailDto(BloodRequest request, boolean isOwner, boolean canManage, Double distanceKm) {
        BloodRequestDetailDto dto = new BloodRequestDetailDto();
        dto.setId(request.getId());
        dto.setBloodGroup(request.getBloodGroup());
        dto.setUnitsRequired(request.getUnitsRequired());
        dto.setUrgency(request.getUrgency());
        dto.setStatus(request.getStatus());
        dto.setHospitalName(request.getHospitalName());
        dto.setHospitalAddress(request.getHospitalAddress());
        dto.setCity(request.getCity());
        dto.setState(request.getState());
        dto.setPostalCode(request.getPostalCode());
        dto.setRequiredBy(request.getRequiredBy());
        dto.setCreatedAt(request.getCreatedAt());
        dto.setDistanceKm(distanceKm);
        dto.setIsOwner(isOwner);
        dto.setCanManage(canManage);

        // Privacy controls: only authorized managers (owner or admin) can view requesterUserId, exact coordinates, description, and terminal metadata
        if (canManage) {
            dto.setRequesterUserId(request.getRequesterUserId());
            dto.setLatitude(request.getLatitude());
            dto.setLongitude(request.getLongitude());
            dto.setDescription(request.getDescription());
            dto.setUpdatedAt(request.getUpdatedAt());
            dto.setCancelledAt(request.getCancelledAt());
            dto.setCancellationReason(request.getCancellationReason());
            dto.setFulfilledAt(request.getFulfilledAt());
        }

        return dto;
    }
}
