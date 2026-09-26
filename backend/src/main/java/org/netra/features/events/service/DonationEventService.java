package org.netra.features.events.service;

import org.netra.core.audit.AuditService;
import org.netra.core.exception.ResourceNotFoundException;
import org.netra.core.exception.UnauthorizedSessionAccessException;
import org.netra.core.exception.ValidationException;
import org.netra.core.security.SecurityUtils;
import org.netra.features.bloodbank.entity.BloodBank;
import org.netra.features.bloodbank.repository.BloodBankRepository;
import org.netra.features.bloodbank.service.BloodBankService;
import org.netra.features.events.dto.*;
import org.netra.features.events.entity.DonationEvent;
import org.netra.features.events.entity.DonationEventRegistrationStatus;
import org.netra.features.events.entity.DonationEventStatus;
import org.netra.features.events.entity.DonationEventType;
import org.netra.features.events.repository.DonationEventRegistrationRepository;
import org.netra.features.events.repository.DonationEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DonationEventService {

    private static final Logger log = LoggerFactory.getLogger(DonationEventService.class);
    private static final int MAX_PAGE_SIZE = 50;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final double MAX_RADIUS_KM = 100.0;
    private static final double MIN_RADIUS_KM = 0.1;

    private final DonationEventRepository donationEventRepository;
    private final DonationEventRegistrationRepository registrationRepository;
    private final BloodBankRepository bloodBankRepository;
    private final DonationEventAuthorizationService authorizationService;
    private final AuditService auditService;

    public DonationEventService(
            DonationEventRepository donationEventRepository,
            DonationEventRegistrationRepository registrationRepository,
            BloodBankRepository bloodBankRepository,
            DonationEventAuthorizationService authorizationService,
            AuditService auditService) {
        this.donationEventRepository = donationEventRepository;
        this.registrationRepository = registrationRepository;
        this.bloodBankRepository = bloodBankRepository;
        this.authorizationService = authorizationService;
        this.auditService = auditService;
    }

    @Transactional
    public DonationEventDetailDto createEvent(CreateDonationEventRequest request, String clientIp, String userAgent) {
        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedSessionAccessException("Authentication is required to create an event."));

        authorizationService.verifyCanCreateEvent(currentUserId, request.getBloodBankId());

        validateEventTimeWindows(
                request.getStartAt(),
                request.getEndAt(),
                request.getRegistrationOpenAt(),
                request.getRegistrationCloseAt()
        );

        DonationEvent event = new DonationEvent();
        event.setBloodBankId(request.getBloodBankId());
        event.setTitle(request.getTitle().trim());
        event.setDescription(request.getDescription() != null ? request.getDescription().trim() : null);
        event.setEventType(DonationEventType.BLOOD_DONATION_CAMP);
        event.setStatus(DonationEventStatus.DRAFT);
        event.setVenueName(request.getVenueName().trim());
        event.setAddress(request.getAddress().trim());
        event.setCity(request.getCity().trim());
        event.setState(request.getState().trim());
        event.setPostalCode(request.getPostalCode().trim());
        event.setLatitude(request.getLatitude());
        event.setLongitude(request.getLongitude());
        event.setStartAt(request.getStartAt());
        event.setEndAt(request.getEndAt());
        event.setRegistrationOpenAt(request.getRegistrationOpenAt());
        event.setRegistrationCloseAt(request.getRegistrationCloseAt());
        event.setDonorCapacity(request.getDonorCapacity());
        event.setCurrentRegistrationCount(0);
        event.setCreatedBy(currentUserId);
        event.setCreatedAt(Instant.now());
        event.setUpdatedAt(Instant.now());

        DonationEvent saved = donationEventRepository.save(event);

        auditService.logAuthEvent(
                "DONATION_EVENT_CREATED",
                currentUserId,
                clientIp,
                userAgent,
                "{\"eventId\":\"" + saved.getId() + "\",\"bloodBankId\":\"" + saved.getBloodBankId() + "\",\"status\":\"DRAFT\"}"
        );

        String bankName = getBloodBankName(saved.getBloodBankId());
        return mapToDetailDto(saved, bankName, null);
    }

    @Transactional
    public DonationEventDetailDto updateEvent(UUID eventId, UpdateDonationEventRequest request, String clientIp, String userAgent) {
        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedSessionAccessException("Authentication is required to update an event."));

        DonationEvent event = donationEventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Donation event not found: " + eventId));

        authorizationService.verifyCanManageEvent(currentUserId, event);

        if (event.getStatus() == DonationEventStatus.COMPLETED || event.getStatus() == DonationEventStatus.CANCELLED) {
            throw new ValidationException("Cannot update an event that is " + event.getStatus() + ".");
        }

        Instant newStart = request.getStartAt() != null ? request.getStartAt() : event.getStartAt();
        Instant newEnd = request.getEndAt() != null ? request.getEndAt() : event.getEndAt();
        Instant newRegOpen = request.getRegistrationOpenAt() != null ? request.getRegistrationOpenAt() : event.getRegistrationOpenAt();
        Instant newRegClose = request.getRegistrationCloseAt() != null ? request.getRegistrationCloseAt() : event.getRegistrationCloseAt();

        validateEventTimeWindows(newStart, newEnd, newRegOpen, newRegClose);

        if (request.getDonorCapacity() != null) {
            if (request.getDonorCapacity() < event.getCurrentRegistrationCount()) {
                throw new ValidationException("Donor capacity cannot be reduced below current registration count ("
                        + event.getCurrentRegistrationCount() + ").");
            }
            event.setDonorCapacity(request.getDonorCapacity());
        }

        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            event.setTitle(request.getTitle().trim());
        }
        if (request.getDescription() != null) {
            event.setDescription(request.getDescription().trim());
        }
        if (request.getVenueName() != null && !request.getVenueName().isBlank()) {
            event.setVenueName(request.getVenueName().trim());
        }
        if (request.getAddress() != null && !request.getAddress().isBlank()) {
            event.setAddress(request.getAddress().trim());
        }
        if (request.getCity() != null && !request.getCity().isBlank()) {
            event.setCity(request.getCity().trim());
        }
        if (request.getState() != null && !request.getState().isBlank()) {
            event.setState(request.getState().trim());
        }
        if (request.getPostalCode() != null && !request.getPostalCode().isBlank()) {
            event.setPostalCode(request.getPostalCode().trim());
        }
        if (request.getLatitude() != null || request.getLongitude() != null) {
            if (request.getLatitude() == null || request.getLongitude() == null) {
                throw new ValidationException("Latitude and longitude must either both be supplied or both be omitted.");
            }
            if (request.getLatitude() < -90.0 || request.getLatitude() > 90.0) {
                throw new ValidationException("Latitude must be between -90.0 and 90.0 degrees.");
            }
            if (request.getLongitude() < -180.0 || request.getLongitude() > 180.0) {
                throw new ValidationException("Longitude must be between -180.0 and 180.0 degrees.");
            }
            event.setLatitude(request.getLatitude());
            event.setLongitude(request.getLongitude());
        }

        event.setStartAt(newStart);
        event.setEndAt(newEnd);
        event.setRegistrationOpenAt(newRegOpen);
        event.setRegistrationCloseAt(newRegClose);
        event.setUpdatedAt(Instant.now());

        DonationEvent updated = donationEventRepository.save(event);

        auditService.logAuthEvent(
                "DONATION_EVENT_UPDATED",
                currentUserId,
                clientIp,
                userAgent,
                "{\"eventId\":\"" + updated.getId() + "\",\"status\":\"" + updated.getStatus() + "\"}"
        );

        String bankName = getBloodBankName(updated.getBloodBankId());
        return mapToDetailDto(updated, bankName, null);
    }

    @Transactional
    public DonationEventDetailDto submitForApproval(UUID eventId, String clientIp, String userAgent) {
        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedSessionAccessException("Authentication is required."));

        DonationEvent event = donationEventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Donation event not found: " + eventId));

        authorizationService.verifyCanManageEvent(currentUserId, event);

        if (event.getStatus() != DonationEventStatus.DRAFT && event.getStatus() != DonationEventStatus.REJECTED) {
            throw new ValidationException("Only DRAFT or REJECTED events can be submitted for approval. Current: " + event.getStatus());
        }

        event.setStatus(DonationEventStatus.PENDING_APPROVAL);
        event.setUpdatedAt(Instant.now());

        DonationEvent saved = donationEventRepository.save(event);

        auditService.logAuthEvent(
                "DONATION_EVENT_SUBMITTED",
                currentUserId,
                clientIp,
                userAgent,
                "{\"eventId\":\"" + saved.getId() + "\",\"status\":\"PENDING_APPROVAL\"}"
        );

        String bankName = getBloodBankName(saved.getBloodBankId());
        return mapToDetailDto(saved, bankName, null);
    }

    @Transactional
    public DonationEventDetailDto reviewEventApproval(
            UUID eventId,
            ApproveDonationEventRequest request,
            String clientIp,
            String userAgent) {

        UUID adminId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedSessionAccessException("Authentication is required."));

        authorizationService.verifyCanApproveEvent(adminId);

        DonationEvent event = donationEventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Donation event not found: " + eventId));

        if (event.getStatus() != DonationEventStatus.PENDING_APPROVAL) {
            throw new ValidationException("Only events in PENDING_APPROVAL status can be reviewed. Current: " + event.getStatus());
        }

        DonationEventStatus targetStatus = request.getStatus();
        if (targetStatus != DonationEventStatus.PUBLISHED && targetStatus != DonationEventStatus.REJECTED) {
            throw new ValidationException("Approval decision must be either PUBLISHED or REJECTED.");
        }

        Instant now = Instant.now();
        event.setStatus(targetStatus);
        event.setUpdatedAt(now);

        String eventType;
        if (targetStatus == DonationEventStatus.PUBLISHED) {
            event.setPublishedAt(now);
            eventType = "DONATION_EVENT_PUBLISHED";
        } else {
            eventType = "DONATION_EVENT_REJECTED";
        }

        DonationEvent saved = donationEventRepository.save(event);

        auditService.logAuthEvent(
                eventType,
                adminId,
                clientIp,
                userAgent,
                "{\"eventId\":\"" + saved.getId() + "\",\"decision\":\"" + targetStatus + "\"}"
        );

        String bankName = getBloodBankName(saved.getBloodBankId());
        return mapToDetailDto(saved, bankName, null);
    }

    @Transactional
    public DonationEventDetailDto cancelEvent(
            UUID eventId,
            CancelDonationEventRequest request,
            String clientIp,
            String userAgent) {

        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedSessionAccessException("Authentication is required."));

        DonationEvent event = donationEventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Donation event not found: " + eventId));

        authorizationService.verifyCanManageEvent(currentUserId, event);

        if (event.getStatus() == DonationEventStatus.CANCELLED) {
            throw new ValidationException("Event is already cancelled.");
        }
        if (event.getStatus() == DonationEventStatus.COMPLETED) {
            throw new ValidationException("Cannot cancel an event that is already completed.");
        }

        Instant now = Instant.now();
        event.setStatus(DonationEventStatus.CANCELLED);
        event.setCancelledAt(now);
        event.setCancelledBy(currentUserId);
        event.setUpdatedAt(now);

        DonationEvent saved = donationEventRepository.save(event);

        auditService.logAuthEvent(
                "DONATION_EVENT_CANCELLED",
                currentUserId,
                clientIp,
                userAgent,
                "{\"eventId\":\"" + saved.getId() + "\",\"reason\":\"" + (request != null && request.getReason() != null ? request.getReason() : "") + "\"}"
        );

        String bankName = getBloodBankName(saved.getBloodBankId());
        return mapToDetailDto(saved, bankName, null);
    }

    public static final Set<DonationEventStatus> PUBLIC_STATUSES = Set.of(
            DonationEventStatus.PUBLISHED,
            DonationEventStatus.REGISTRATION_CLOSED,
            DonationEventStatus.ONGOING,
            DonationEventStatus.COMPLETED
    );

    @Transactional(readOnly = true)
    public DonationEventDetailDto getEventById(UUID eventId) {
        DonationEvent event = donationEventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Donation event not found: " + eventId));

        // If not publicly visible, ensure caller is authorized staff/admin
        if (!event.getStatus().isPubliclyVisible()) {
            UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
            if (currentUserId == null) {
                throw new ResourceNotFoundException("Donation event not found: " + eventId);
            }
            authorizationService.verifyCanManageEvent(currentUserId, event);
        }

        String bankName = getBloodBankName(event.getBloodBankId());
        return mapToDetailDto(event, bankName, null);
    }

    @Transactional(readOnly = true)
    public Page<DonationEventSummaryDto> discoverEvents(
            String city,
            UUID bloodBankId,
            DonationEventStatus status,
            Boolean upcomingOnly,
            Pageable pageable) {

        int pageSize = Math.min(Math.max(1, pageable.getPageSize()), MAX_PAGE_SIZE);
        Pageable bounded = PageRequest.of(pageable.getPageNumber(), pageSize, pageable.getSort());
        Instant now = Instant.now();

        UUID currentUserId = SecurityUtils.getCurrentUserId().orElse(null);
        boolean isAdmin = currentUserId != null && SecurityUtils.hasRole("ROLE_ADMIN");
        boolean isAuthorizedForBank = (bloodBankId != null && currentUserId != null)
                && (isAdmin || authorizationService.isAuthorizedToManageBank(currentUserId, bloodBankId));

        Set<DonationEventStatus> allowedStatuses;

        if (bloodBankId != null) {
            // Case B: bloodBankId != null
            if (isAuthorizedForBank) {
                // Authenticated user authorized to manage that blood bank (or admin): all statuses allowed
                if (status != null) {
                    allowedStatuses = Set.of(status);
                } else {
                    allowedStatuses = Set.of(DonationEventStatus.values());
                }
            } else {
                // Unauthenticated/normal user or staff from another blood bank
                boolean isStaffFromAnotherBank = currentUserId != null && SecurityUtils.hasRole("ROLE_BLOODBANK");
                if (status != null) {
                    if (status.isPubliclyVisible()) {
                        allowedStatuses = Set.of(status);
                    } else {
                        // Private status explicitly requested
                        if (isStaffFromAnotherBank) {
                            // Staff from another blood bank explicitly requesting private status -> HTTP 403
                            authorizationService.verifyCanManageBloodBank(currentUserId, bloodBankId);
                        }
                        // Unauthenticated or normal user requesting private status -> return empty page
                        return Page.empty(bounded);
                    }
                } else {
                    // No private status requested -> return public events only
                    allowedStatuses = PUBLIC_STATUSES;
                }
            }
        } else {
            // Case A: bloodBankId == null
            if (isAdmin) {
                if (status != null) {
                    allowedStatuses = Set.of(status);
                } else {
                    allowedStatuses = Set.of(DonationEventStatus.values());
                }
            } else {
                // unauthenticated or normal users (including staff without bloodBankId specified)
                if (status != null) {
                    if (status.isPubliclyVisible()) {
                        allowedStatuses = Set.of(status);
                    } else {
                        return Page.empty(bounded);
                    }
                } else {
                    allowedStatuses = PUBLIC_STATUSES;
                }
            }
        }

        Page<DonationEvent> events;
        if (bloodBankId != null) {
            if (Boolean.TRUE.equals(upcomingOnly)) {
                events = donationEventRepository.findUpcomingEventsByBloodBankId(bloodBankId, allowedStatuses, now, bounded);
            } else if (status != null) {
                events = donationEventRepository.findByBloodBankIdAndStatus(bloodBankId, status, bounded);
            } else {
                events = donationEventRepository.findByBloodBankIdAndStatusIn(bloodBankId, allowedStatuses, bounded);
            }
        } else if (city != null && !city.isBlank()) {
            events = Boolean.TRUE.equals(upcomingOnly)
                    ? donationEventRepository.findUpcomingEventsByCity(city.trim(), allowedStatuses, now, bounded)
                    : donationEventRepository.findByCityIgnoreCaseAndStatusIn(city.trim(), allowedStatuses, bounded);
        } else if (Boolean.TRUE.equals(upcomingOnly)) {
            events = donationEventRepository.findUpcomingEvents(allowedStatuses, now, bounded);
        } else {
            events = donationEventRepository.findByStatusIn(allowedStatuses, bounded);
        }

        Map<UUID, String> bankNameMap = new HashMap<>();
        return events.map(e -> {
            String bName = bankNameMap.computeIfAbsent(e.getBloodBankId(), this::getBloodBankName);
            return mapToSummaryDto(e, bName, null);
        });
    }

    /**
     * Finds nearby donation camps within the given radius (in kilometers) from the provided coordinates.
     *
     * DESIGN RATIONALE - STATUS FILTERING:
     * Proximity-based nearby discovery is explicitly tailored for prospective donors seeking active,
     * joinable blood donation camps in their vicinity. Therefore, this query intentionally filters
     * exclusively by {@link DonationEventStatus#PUBLISHED} (events open for registration).
     *
     * It deliberately does NOT include {@code REGISTRATION_CLOSED}, {@code ONGOING}, or {@code COMPLETED}
     * events. Returning closed or past drives in nearby search would degrade donor user experience by
     * presenting events that can no longer accept registrations. Donors or organizers requiring broader
     * historical or city-wide listings use the general discovery endpoint {@code /api/v1/donation-events}.
     */
    @Transactional(readOnly = true)
    public List<DonationEventSummaryDto> findNearbyEvents(double latitude, double longitude, double radiusKm) {
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

        // Intentional design choice: nearby candidate search only queries PUBLISHED camps open for registration
        List<DonationEvent> candidates = donationEventRepository.findNearbyCandidates(
                minLat, maxLat, minLng, maxLng, DonationEventStatus.PUBLISHED);

        Map<UUID, String> bankNameMap = new HashMap<>();

        return candidates.stream()
                .map(event -> {
                    double dist = BloodBankService.calculateHaversineDistanceKm(latitude, longitude, event.getLatitude(), event.getLongitude());
                    return new AbstractMap.SimpleEntry<>(event, dist);
                })
                .filter(entry -> entry.getValue() <= radiusKm)
                .sorted(Comparator.comparingDouble(Map.Entry::getValue))
                .map(entry -> {
                    String bName = bankNameMap.computeIfAbsent(entry.getKey().getBloodBankId(), this::getBloodBankName);
                    return mapToSummaryDto(entry.getKey(), bName, Math.round(entry.getValue() * 10.0) / 10.0);
                })
                .collect(Collectors.toList());
    }

    /**
     * Reconciles denormalized performance counter current_registration_count with actual DB slot-consuming rows.
     */
    @Transactional
    public void reconcileRegistrationCount(UUID eventId) {
        DonationEvent event = donationEventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Donation event not found: " + eventId));

        Set<DonationEventRegistrationStatus> slotConsumingStatuses = Set.of(
                DonationEventRegistrationStatus.REGISTERED,
                DonationEventRegistrationStatus.CHECKED_IN
        );

        long actualCount = registrationRepository.countSlotConsumingRegistrations(eventId, slotConsumingStatuses);

        if (event.getCurrentRegistrationCount() != (int) actualCount) {
            log.warn("Reconciliation mismatch for event {}: cached count = {}, actual slot-consuming count = {}. Syncing.",
                    eventId, event.getCurrentRegistrationCount(), actualCount);
            event.setCurrentRegistrationCount((int) actualCount);
            event.setUpdatedAt(Instant.now());
            donationEventRepository.save(event);
        }
    }

    private void validateEventTimeWindows(Instant start, Instant end, Instant regOpen, Instant regClose) {
        if (start == null || end == null || regOpen == null || regClose == null) {
            throw new ValidationException("Start, end, registration open, and registration close times are all required.");
        }
        if (!start.isBefore(end)) {
            throw new ValidationException("Event start time must be before event end time.");
        }
        if (!regOpen.isBefore(regClose)) {
            throw new ValidationException("Registration open time must be before registration close time.");
        }
        if (regClose.isAfter(start)) {
            throw new ValidationException("Registration must close before or when the event starts.");
        }
    }

    private String getBloodBankName(UUID bloodBankId) {
        return bloodBankRepository.findById(bloodBankId)
                .map(BloodBank::getName)
                .orElse("NETRA Blood Centre");
    }

    private DonationEventSummaryDto mapToSummaryDto(DonationEvent e, String bloodBankName, Double distanceKm) {
        return new DonationEventSummaryDto(
                e.getId(),
                e.getBloodBankId(),
                bloodBankName,
                e.getTitle(),
                e.getDescription(),
                e.getEventType(),
                e.getStatus(),
                e.getVenueName(),
                e.getAddress(),
                e.getCity(),
                e.getState(),
                e.getPostalCode(),
                e.getStartAt(),
                e.getEndAt(),
                e.getRegistrationOpenAt(),
                e.getRegistrationCloseAt(),
                e.getDonorCapacity(),
                e.getCurrentRegistrationCount(),
                distanceKm
        );
    }

    private DonationEventDetailDto mapToDetailDto(DonationEvent e, String bloodBankName, Double distanceKm) {
        return new DonationEventDetailDto(
                e.getId(),
                e.getBloodBankId(),
                bloodBankName,
                e.getTitle(),
                e.getDescription(),
                e.getEventType(),
                e.getStatus(),
                e.getVenueName(),
                e.getAddress(),
                e.getCity(),
                e.getState(),
                e.getPostalCode(),
                e.getLatitude(),
                e.getLongitude(),
                e.getStartAt(),
                e.getEndAt(),
                e.getRegistrationOpenAt(),
                e.getRegistrationCloseAt(),
                e.getDonorCapacity(),
                e.getCurrentRegistrationCount(),
                distanceKm,
                e.getPublishedAt(),
                e.getCancelledAt(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}
