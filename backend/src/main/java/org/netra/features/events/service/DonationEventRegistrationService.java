package org.netra.features.events.service;

import org.netra.core.audit.AuditService;
import org.netra.core.exception.*;
import org.netra.core.security.SecurityUtils;
import org.netra.features.events.dto.DonationEventRegistrationDto;
import org.netra.features.events.dto.EventAttendeeDto;
import org.netra.features.events.entity.DonationEvent;
import org.netra.features.events.entity.DonationEventRegistration;
import org.netra.features.events.entity.DonationEventRegistrationStatus;
import org.netra.features.events.entity.DonationEventStatus;
import org.netra.features.events.repository.DonationEventRegistrationRepository;
import org.netra.features.events.repository.DonationEventRepository;
import org.netra.features.user.entity.User;
import org.netra.features.user.entity.UserStatus;
import org.netra.features.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DonationEventRegistrationService {

    private static final Logger log = LoggerFactory.getLogger(DonationEventRegistrationService.class);

    private final DonationEventRepository donationEventRepository;
    private final DonationEventRegistrationRepository registrationRepository;
    private final UserRepository userRepository;
    private final DonationEventAuthorizationService authorizationService;
    private final AuditService auditService;

    public DonationEventRegistrationService(
            DonationEventRepository donationEventRepository,
            DonationEventRegistrationRepository registrationRepository,
            UserRepository userRepository,
            DonationEventAuthorizationService authorizationService,
            AuditService auditService) {
        this.donationEventRepository = donationEventRepository;
        this.registrationRepository = registrationRepository;
        this.userRepository = userRepository;
        this.authorizationService = authorizationService;
        this.auditService = auditService;
    }

    @Transactional
    public DonationEventRegistrationDto registerForEvent(UUID eventId, String clientIp, String userAgent) {
        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedSessionAccessException("Authentication is required to register for an event."));

        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UnauthorizedSessionAccessException("User not found: " + currentUserId));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AccountStatusException("Your account is " + user.getStatus() + ". Only active accounts can register.");
        }

        DonationEvent event = donationEventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Donation event not found: " + eventId));

        if (event.getStatus() != DonationEventStatus.PUBLISHED) {
            throw new EventRegistrationClosedException("Registration is not available. Event status: " + event.getStatus());
        }

        Instant now = Instant.now();
        if (now.isBefore(event.getRegistrationOpenAt())) {
            throw new EventRegistrationClosedException("Registration for this donation camp has not opened yet.");
        }
        if (now.isAfter(event.getRegistrationCloseAt())) {
            throw new EventRegistrationClosedException("Registration for this donation camp is closed.");
        }

        Optional<DonationEventRegistration> existingOpt = registrationRepository.findByEventIdAndDonorUserId(eventId, currentUserId);

        if (existingOpt.isPresent()) {
            DonationEventRegistration existing = existingOpt.get();
            if (existing.getStatus() == DonationEventRegistrationStatus.REGISTERED ||
                existing.getStatus() == DonationEventRegistrationStatus.CHECKED_IN) {
                throw new DuplicateRegistrationException("You are already registered for this donation camp.");
            }

            if (existing.getStatus() == DonationEventRegistrationStatus.CANCELLED) {
                // Reactivation: atomic slot acquisition
                int rows = donationEventRepository.acquireRegistrationSlot(eventId, now);
                if (rows == 0) {
                    auditService.logAuthEvent(
                            "DONATION_EVENT_CAPACITY_REACHED",
                            currentUserId,
                            clientIp,
                            userAgent,
                            "{\"eventId\":\"" + eventId + "\",\"capacity\":\"" + event.getDonorCapacity() + "\"}"
                    );
                    throw new EventFullException("Donation camp has reached full capacity.");
                }

                existing.setStatus(DonationEventRegistrationStatus.REGISTERED);
                existing.setRegisteredAt(now);
                existing.setCancelledAt(null);
                existing.setUpdatedAt(now);
                DonationEventRegistration reactivated = registrationRepository.saveAndFlush(existing);

                auditService.logAuthEvent(
                        "DONATION_EVENT_REGISTRATION_CREATED",
                        currentUserId,
                        clientIp,
                        userAgent,
                        "{\"registrationId\":\"" + reactivated.getId() + "\",\"eventId\":\"" + eventId + "\",\"action\":\"REACTIVATED\"}"
                );

                return mapToDto(reactivated, event.getTitle());
            }
        }

        // New registration: atomic slot acquisition
        int rows = donationEventRepository.acquireRegistrationSlot(eventId, now);
        if (rows == 0) {
            auditService.logAuthEvent(
                    "DONATION_EVENT_CAPACITY_REACHED",
                    currentUserId,
                    clientIp,
                    userAgent,
                    "{\"eventId\":\"" + eventId + "\",\"capacity\":\"" + event.getDonorCapacity() + "\"}"
            );
            throw new EventFullException("Donation camp has reached full capacity.");
        }

        DonationEventRegistration registration = new DonationEventRegistration(
                eventId, currentUserId, DonationEventRegistrationStatus.REGISTERED);

        try {
            DonationEventRegistration saved = registrationRepository.saveAndFlush(registration);

            auditService.logAuthEvent(
                    "DONATION_EVENT_REGISTRATION_CREATED",
                    currentUserId,
                    clientIp,
                    userAgent,
                    "{\"registrationId\":\"" + saved.getId() + "\",\"eventId\":\"" + eventId + "\"}"
            );

            return mapToDto(saved, event.getTitle());
        } catch (DataIntegrityViolationException ex) {
            if (isEventDonorUniqueConstraintViolation(ex)) {
                // Concurrent double-tap attempt: transaction rollback will safely restore slot counter in DB
                log.warn("Concurrent duplicate registration attempt for event {} by user {}", eventId, currentUserId);
                throw new DuplicateRegistrationException("You are already registered for this donation camp.");
            }
            log.error("Data integrity violation during registration for event {} by user {}: {}", eventId, currentUserId, ex.getMessage());
            throw ex;
        }
    }

    private boolean isEventDonorUniqueConstraintViolation(DataIntegrityViolationException ex) {
        // 1. Inspect nested Hibernate ConstraintViolationException if available
        Throwable current = ex;
        while (current != null) {
            if (current instanceof org.hibernate.exception.ConstraintViolationException cve) {
                String constraint = cve.getConstraintName();
                if (constraint != null && constraint.toLowerCase().contains("uq_event_donor")) {
                    return true;
                }
            }
            if (current instanceof java.sql.SQLException sqlEx) {
                String sqlMsg = sqlEx.getMessage();
                if (sqlMsg != null && sqlMsg.toLowerCase().contains("uq_event_donor")) {
                    return true;
                }
            }
            current = current.getCause();
        }

        // 2. Safe message-based fallback for H2 and diverse JDBC drivers
        String msg = ex.getMessage() != null ? ex.getMessage() : "";
        Throwable mostSpecific = ex.getMostSpecificCause();
        String mostSpecificMsg = (mostSpecific != null && mostSpecific.getMessage() != null)
                ? mostSpecific.getMessage()
                : "";
        String combined = (msg + " " + mostSpecificMsg).toLowerCase();

        if (combined.contains("uq_event_donor")) {
            return true;
        }

        boolean isUniqueViolation = combined.contains("unique") || combined.contains("23505") || combined.contains("duplicate");
        boolean isRegistrationTable = combined.contains("donation_event_registrations");
        boolean hasCompositeKeys = combined.contains("event_id") && combined.contains("donor_user_id");

        return isUniqueViolation && isRegistrationTable && hasCompositeKeys;
    }

    @Transactional
    public DonationEventRegistrationDto cancelRegistration(UUID eventId, String clientIp, String userAgent) {
        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedSessionAccessException("Authentication is required."));

        DonationEventRegistration reg = registrationRepository.findByEventIdAndDonorUserId(eventId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration not found for event: " + eventId));

        if (reg.getStatus() == DonationEventRegistrationStatus.CANCELLED) {
            DonationEvent event = donationEventRepository.findById(eventId).orElse(null);
            String title = event != null ? event.getTitle() : "Donation Camp";
            return mapToDto(reg, title);
        }

        if (reg.getStatus() == DonationEventRegistrationStatus.COMPLETED) {
            throw new ValidationException("Cannot cancel a completed donation registration.");
        }

        Instant now = Instant.now();
        if (reg.getStatus().consumesCapacitySlot()) {
            donationEventRepository.releaseRegistrationSlot(eventId, now);
        }

        reg.setStatus(DonationEventRegistrationStatus.CANCELLED);
        reg.setCancelledAt(now);
        reg.setUpdatedAt(now);

        DonationEventRegistration saved = registrationRepository.saveAndFlush(reg);

        auditService.logAuthEvent(
                "DONATION_EVENT_REGISTRATION_CANCELLED",
                currentUserId,
                clientIp,
                userAgent,
                "{\"registrationId\":\"" + saved.getId() + "\",\"eventId\":\"" + eventId + "\"}"
        );

        DonationEvent event = donationEventRepository.findById(eventId).orElse(null);
        String title = event != null ? event.getTitle() : "Donation Camp";
        return mapToDto(saved, title);
    }

    @Transactional(readOnly = true)
    public DonationEventRegistrationDto getOwnRegistration(UUID eventId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedSessionAccessException("Authentication is required."));

        DonationEventRegistration reg = registrationRepository.findByEventIdAndDonorUserId(eventId, currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Registration not found for event: " + eventId));

        DonationEvent event = donationEventRepository.findById(eventId).orElse(null);
        String title = event != null ? event.getTitle() : "Donation Camp";
        return mapToDto(reg, title);
    }

    @Transactional(readOnly = true)
    public List<DonationEventRegistrationDto> getMyRegistrations() {
        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedSessionAccessException("Authentication is required."));

        List<DonationEventRegistration> regs = registrationRepository.findByDonorUserIdOrderByRegisteredAtDesc(currentUserId);
        Map<UUID, String> eventTitleMap = new HashMap<>();

        return regs.stream()
                .map(r -> {
                    String title = eventTitleMap.computeIfAbsent(r.getEventId(), id ->
                            donationEventRepository.findById(id).map(DonationEvent::getTitle).orElse("Donation Camp"));
                    return mapToDto(r, title);
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EventAttendeeDto> getEventAttendees(UUID eventId) {
        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedSessionAccessException("Authentication is required."));

        DonationEvent event = donationEventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Donation event not found: " + eventId));

        // Enforce organizer / admin access ONLY
        authorizationService.verifyCanViewAttendeeList(currentUserId, event);

        List<DonationEventRegistration> registrations = registrationRepository.findByEventIdOrderByRegisteredAtDesc(eventId);
        Map<UUID, String> userNameMap = new HashMap<>();

        return registrations.stream()
                .map(r -> {
                    String donorName = userNameMap.computeIfAbsent(r.getDonorUserId(), id ->
                            userRepository.findById(id).map(User::getFullName).orElse("Anonymous Donor"));
                    return new EventAttendeeDto(
                            r.getId(),
                            r.getDonorUserId(),
                            donorName,
                            r.getStatus(),
                            r.getRegisteredAt(),
                            r.getCheckedInAt()
                    );
                })
                .collect(Collectors.toList());
    }

    private DonationEventRegistrationDto mapToDto(DonationEventRegistration r, String eventTitle) {
        return new DonationEventRegistrationDto(
                r.getId(),
                r.getEventId(),
                eventTitle,
                r.getDonorUserId(),
                r.getStatus(),
                r.getRegisteredAt(),
                r.getCancelledAt(),
                r.getCheckedInAt(),
                r.getCompletedAt()
        );
    }
}
