package org.netra.features.karma.controller;

import jakarta.validation.Valid;
import org.netra.core.exception.UnauthorizedSessionAccessException;
import org.netra.core.security.SecurityUtils;
import org.netra.features.karma.dto.DisputeReversalRequest;
import org.netra.features.karma.dto.KarmaSummaryDto;
import org.netra.features.karma.dto.KarmaTransactionDto;
import org.netra.features.karma.entity.KarmaTransaction;
import org.netra.features.karma.service.KarmaService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/karma")
public class KarmaController {

    private final KarmaService karmaService;

    public KarmaController(KarmaService karmaService) {
        this.karmaService = karmaService;
    }

    /**
     * Get Karma balance and summary for the currently authenticated user.
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<KarmaSummaryDto> getMyKarma() {
        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedSessionAccessException("User is not authenticated."));
        return ResponseEntity.ok(karmaService.getKarmaSummary(currentUserId));
    }

    /**
     * Get paginated Karma transaction history for the authenticated user.
     */
    @GetMapping("/transactions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<KarmaTransactionDto>> getMyTransactions(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        UUID currentUserId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedSessionAccessException("User is not authenticated."));
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(50, Math.max(1, size)));
        return ResponseEntity.ok(karmaService.getTransactionHistory(currentUserId, pageable));
    }

    /**
     * Reverse a disputed or incorrect Karma transaction (Admin only).
     */
    @PostMapping("/transactions/{id}/reverse")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<KarmaTransactionDto> reverseTransaction(
            @PathVariable("id") UUID transactionId,
            @Valid @RequestBody DisputeReversalRequest request) {
        UUID adminId = SecurityUtils.getCurrentUserId()
                .orElseThrow(() -> new UnauthorizedSessionAccessException("User is not authenticated."));
        KarmaTransaction tx = karmaService.reverseTransaction(transactionId, request.getReason(), adminId);
        return ResponseEntity.ok(KarmaTransactionDto.fromEntity(tx));
    }
}
