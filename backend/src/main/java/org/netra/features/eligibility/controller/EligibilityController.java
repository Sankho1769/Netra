package org.netra.features.eligibility.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.netra.core.ratelimit.RateLimitingService;
import org.netra.core.security.ClientIpResolver;
import org.netra.features.eligibility.dto.*;
import org.netra.features.eligibility.service.EligibilityService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/eligibility")
public class EligibilityController {

    private final EligibilityService eligibilityService;
    private final RateLimitingService rateLimitingService;
    private final ClientIpResolver clientIpResolver;

    public EligibilityController(
            EligibilityService eligibilityService,
            RateLimitingService rateLimitingService,
            ClientIpResolver clientIpResolver) {
        this.eligibilityService = eligibilityService;
        this.rateLimitingService = rateLimitingService;
        this.clientIpResolver = clientIpResolver;
    }

    @GetMapping("/rules/version")
    public ResponseEntity<RuleVersionResponse> getRuleVersion() {
        return ResponseEntity.ok(eligibilityService.getActiveRuleVersion());
    }

    @GetMapping("/questions")
    public ResponseEntity<QuestionnaireResponse> getQuestions() {
        return ResponseEntity.ok(eligibilityService.getActiveQuestions());
    }

    @PostMapping("/sessions")
    public ResponseEntity<SessionResponse> createSession(
            @RequestBody(required = false) CreateSessionRequest request,
            HttpServletRequest servletRequest) {

        String clientIp = clientIpResolver.resolveClientIp(servletRequest);
        rateLimitingService.checkRateLimit(clientIp);

        SessionResponse session = eligibilityService.createSession(
                request != null ? request : new CreateSessionRequest(),
                clientIp,
                servletRequest.getHeader("User-Agent")
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(session);
    }

    @PostMapping("/sessions/{sessionId}/answers")
    public ResponseEntity<Map<String, Object>> submitAnswers(
            @PathVariable UUID sessionId,
            @RequestHeader(value = "X-Capability-Token", required = false) String capabilityToken,
            @Valid @RequestBody SubmitAnswersRequest request,
            HttpServletRequest servletRequest) {

        String clientIp = clientIpResolver.resolveClientIp(servletRequest);
        rateLimitingService.checkRateLimit(clientIp);

        int savedCount = eligibilityService.submitAnswers(sessionId, request, capabilityToken);
        return ResponseEntity.ok(Map.of("status", "SUCCESS", "savedCount", savedCount));
    }

    @PostMapping("/sessions/{sessionId}/check")
    public ResponseEntity<EligibilityResultResponse> checkEligibility(
            @PathVariable UUID sessionId,
            @RequestHeader(value = "X-Capability-Token", required = false) String capabilityToken,
            @RequestBody(required = false) CheckEligibilityRequest request,
            HttpServletRequest servletRequest) {

        String clientIp = clientIpResolver.resolveClientIp(servletRequest);
        rateLimitingService.checkRateLimit(clientIp);

        EligibilityResultResponse result = eligibilityService.evaluateSession(
                sessionId,
                request != null ? request : new CheckEligibilityRequest(),
                clientIp,
                servletRequest.getHeader("User-Agent"),
                capabilityToken
        );
        return ResponseEntity.ok(result);
    }

    @GetMapping("/sessions/{sessionId}/result")
    public ResponseEntity<EligibilityResultResponse> getResult(
            @PathVariable UUID sessionId,
            @RequestHeader(value = "X-Capability-Token", required = false) String capabilityToken,
            HttpServletRequest servletRequest) {

        String clientIp = clientIpResolver.resolveClientIp(servletRequest);
        rateLimitingService.checkRateLimit(clientIp);

        return ResponseEntity.ok(eligibilityService.getSessionResult(sessionId, capabilityToken));
    }
}
