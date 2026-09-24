package org.netra.core.observability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.UUID;

/**
 * Standardized structured logging utility for operationally important events.
 * Emits key-value operational telemetry into application logs while strictly
 * preventing logging of sensitive data (credentials, tokens, medical information, PII).
 */
public final class StructuredLogger {

    private static final Logger log = LoggerFactory.getLogger(StructuredLogger.class);

    private StructuredLogger() {
    }

    public static void logOperation(
            String eventName,
            UUID userId,
            String role,
            String resourceType,
            UUID resourceId,
            String operation,
            Long durationMs,
            String outcome) {

        String correlationId = CorrelationIdFilter.getCurrentCorrelationId();
        String safeUserId = userId != null ? userId.toString() : "ANONYMOUS";
        String safeRole = role != null ? role : "NONE";
        String safeResourceType = resourceType != null ? resourceType : "NONE";
        String safeResourceId = resourceId != null ? resourceId.toString() : "NONE";
        String safeDuration = durationMs != null ? durationMs + "ms" : "N/A";
        String safeOutcome = outcome != null ? outcome : "SUCCESS";

        log.info("event=\"{}\" correlationId=\"{}\" userId=\"{}\" role=\"{}\" resource=\"{}\" resourceId=\"{}\" operation=\"{}\" duration=\"{}\" outcome=\"{}\"",
                eventName,
                correlationId,
                safeUserId,
                safeRole,
                safeResourceType,
                safeResourceId,
                operation,
                safeDuration,
                safeOutcome
        );
    }

    public static void logSecurityEvent(
            String eventName,
            UUID userId,
            String role,
            String operation,
            String outcome,
            String reason) {

        String correlationId = CorrelationIdFilter.getCurrentCorrelationId();
        String safeUserId = userId != null ? userId.toString() : "ANONYMOUS";
        String safeRole = role != null ? role : "NONE";
        String safeReason = reason != null ? sanitize(reason) : "NONE";

        log.warn("event=\"{}\" correlationId=\"{}\" userId=\"{}\" role=\"{}\" operation=\"{}\" outcome=\"{}\" reason=\"{}\"",
                eventName,
                correlationId,
                safeUserId,
                safeRole,
                operation,
                outcome,
                safeReason
        );
    }

    private static String sanitize(String input) {
        if (input == null) return "";
        // Strip out control characters and quotes to preserve log format integrity
        return input.replace("\"", "'").replace("\n", " ").replace("\r", " ").trim();
    }
}
