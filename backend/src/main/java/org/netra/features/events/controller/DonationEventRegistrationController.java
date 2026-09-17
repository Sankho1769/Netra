package org.netra.features.events.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.netra.core.security.ClientIpResolver;
import org.netra.features.events.dto.DonationEventRegistrationDto;
import org.netra.features.events.dto.EventAttendeeDto;
import org.netra.features.events.service.DonationEventRegistrationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/donation-events")
public class DonationEventRegistrationController {

    private final DonationEventRegistrationService registrationService;
    private final ClientIpResolver clientIpResolver;

    public DonationEventRegistrationController(
            DonationEventRegistrationService registrationService,
            ClientIpResolver clientIpResolver) {
        this.registrationService = registrationService;
        this.clientIpResolver = clientIpResolver;
    }

    @PostMapping("/{id}/register")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DonationEventRegistrationDto> registerForEvent(
            @PathVariable UUID id,
            HttpServletRequest servletRequest) {

        String clientIp = clientIpResolver.resolveClientIp(servletRequest);
        String userAgent = servletRequest.getHeader("User-Agent");

        DonationEventRegistrationDto registration = registrationService.registerForEvent(id, clientIp, userAgent);
        return ResponseEntity.status(HttpStatus.CREATED).body(registration);
    }

    @DeleteMapping("/{id}/registration")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DonationEventRegistrationDto> cancelRegistration(
            @PathVariable UUID id,
            HttpServletRequest servletRequest) {

        String clientIp = clientIpResolver.resolveClientIp(servletRequest);
        String userAgent = servletRequest.getHeader("User-Agent");

        DonationEventRegistrationDto cancelled = registrationService.cancelRegistration(id, clientIp, userAgent);
        return ResponseEntity.ok(cancelled);
    }

    @GetMapping("/{id}/registration")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DonationEventRegistrationDto> getOwnRegistration(@PathVariable UUID id) {
        DonationEventRegistrationDto registration = registrationService.getOwnRegistration(id);
        return ResponseEntity.ok(registration);
    }

    @GetMapping("/my-registrations")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<DonationEventRegistrationDto>> getMyRegistrations() {
        List<DonationEventRegistrationDto> list = registrationService.getMyRegistrations();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}/registrations")
    @PreAuthorize("hasAnyRole('ADMIN', 'BLOODBANK')")
    public ResponseEntity<List<EventAttendeeDto>> getEventAttendees(@PathVariable UUID id) {
        List<EventAttendeeDto> attendees = registrationService.getEventAttendees(id);
        return ResponseEntity.ok(attendees);
    }
}
