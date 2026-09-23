package org.netra.features.fulfillment.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.netra.features.fulfillment.dto.*;
import org.netra.features.fulfillment.service.FulfillmentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/fulfillments")
public class FulfillmentController {

    private final FulfillmentService fulfillmentService;

    public FulfillmentController(FulfillmentService fulfillmentService) {
        this.fulfillmentService = fulfillmentService;
    }

    @PostMapping
    public ResponseEntity<FulfillmentDto> createFulfillment(
            @Valid @RequestBody CreateFulfillmentRequest request,
            HttpServletRequest httpRequest) {
        String clientIp = httpRequest.getRemoteAddr();
        String userAgent = httpRequest.getHeader("User-Agent");
        FulfillmentDto dto = fulfillmentService.createFulfillment(request, clientIp, userAgent);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PostMapping("/{id}/start")
    @PreAuthorize("hasAnyRole('BLOODBANK', 'ADMIN')")
    public ResponseEntity<FulfillmentDto> startFulfillment(
            @PathVariable UUID id,
            HttpServletRequest httpRequest) {
        String clientIp = httpRequest.getRemoteAddr();
        String userAgent = httpRequest.getHeader("User-Agent");
        FulfillmentDto dto = fulfillmentService.startFulfillment(id, clientIp, userAgent);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('BLOODBANK', 'ADMIN')")
    public ResponseEntity<FulfillmentDto> completeFulfillment(
            @PathVariable UUID id,
            HttpServletRequest httpRequest) {
        String clientIp = httpRequest.getRemoteAddr();
        String userAgent = httpRequest.getHeader("User-Agent");
        FulfillmentDto dto = fulfillmentService.completeFulfillment(id, clientIp, userAgent);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/{id}/fail")
    @PreAuthorize("hasAnyRole('BLOODBANK', 'ADMIN')")
    public ResponseEntity<FulfillmentDto> failFulfillment(
            @PathVariable UUID id,
            @Valid @RequestBody FailFulfillmentRequest request,
            HttpServletRequest httpRequest) {
        String clientIp = httpRequest.getRemoteAddr();
        String userAgent = httpRequest.getHeader("User-Agent");
        FulfillmentDto dto = fulfillmentService.failFulfillment(id, request, clientIp, userAgent);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<FulfillmentDto> cancelFulfillment(
            @PathVariable UUID id,
            @Valid @RequestBody CancelFulfillmentRequest request,
            HttpServletRequest httpRequest) {
        String clientIp = httpRequest.getRemoteAddr();
        String userAgent = httpRequest.getHeader("User-Agent");
        FulfillmentDto dto = fulfillmentService.cancelFulfillment(id, request, clientIp, userAgent);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/my")
    public ResponseEntity<Page<FulfillmentDto>> getMyFulfillments(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<FulfillmentDto> page = fulfillmentService.getMyFulfillments(pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('BLOODBANK', 'ADMIN')")
    public ResponseEntity<Page<FulfillmentDto>> getPendingFulfillments(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<FulfillmentDto> page = fulfillmentService.getPendingFulfillments(pageable);
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FulfillmentDetailDto> getFulfillmentById(@PathVariable UUID id) {
        FulfillmentDetailDto dto = fulfillmentService.getFulfillmentById(id);
        return ResponseEntity.ok(dto);
    }
}
