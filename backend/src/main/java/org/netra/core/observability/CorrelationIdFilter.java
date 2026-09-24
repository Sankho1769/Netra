package org.netra.core.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Filter responsible for assigning, validating, and propagating correlation IDs for all incoming HTTP requests.
 *
 * Requirements:
 * - Every incoming request is assigned a correlation ID.
 * - Client-supplied correlation IDs (X-Correlation-ID or X-Request-ID) are validated against a strict safe pattern.
 * - Unsafe or missing values are replaced with a secure random UUID.
 * - The correlation ID is added to SLF4J MDC, request attributes, and the HTTP response header.
 * - MDC is guaranteed to be cleaned up after request processing.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String REQUEST_ID_HEADER = "X-Request-ID";
    public static final String MDC_KEY = "correlationId";
    public static final String REQUEST_ATTR = "org.netra.correlationId";

    // Strictly accept alphanumeric, hyphen, and underscore characters between 8 and 64 characters
    private static final Pattern SAFE_CORRELATION_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{8,64}$");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String correlationId = resolveCorrelationId(request);

        MDC.put(MDC_KEY, correlationId);
        request.setAttribute(REQUEST_ATTR, correlationId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    /**
     * Resolves and validates a safe correlation ID from headers or generates a new UUID.
     */
    private String resolveCorrelationId(HttpServletRequest request) {
        String clientProvided = request.getHeader(CORRELATION_ID_HEADER);
        if (clientProvided == null || clientProvided.isBlank()) {
            clientProvided = request.getHeader(REQUEST_ID_HEADER);
        }

        if (clientProvided != null && isValid(clientProvided.trim())) {
            return clientProvided.trim();
        }

        return UUID.randomUUID().toString();
    }

    private boolean isValid(String candidate) {
        return SAFE_CORRELATION_ID_PATTERN.matcher(candidate).matches();
    }

    /**
     * Retrieves the current correlation ID from MDC, or returns "SYSTEM" / fallback if none is active.
     */
    public static String getCurrentCorrelationId() {
        String corrId = MDC.get(MDC_KEY);
        return (corrId != null && !corrId.isBlank()) ? corrId : "SYSTEM";
    }
}
