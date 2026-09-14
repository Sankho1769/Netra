package org.netra.core.security;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Secure client IP resolver.
 * Protects against rate-limiting bypass via forged X-Forwarded-For headers.
 * X-Forwarded-For is only trusted if the immediate upstream peer matches a configured trusted proxy.
 */
@Component
public class ClientIpResolver {

    private static final Logger log = LoggerFactory.getLogger(ClientIpResolver.class);

    private final Set<String> trustedProxies;

    public ClientIpResolver(
            @Value("${netra.security.trusted-proxies:127.0.0.1,0:0:0:0:0:0:0:1}") String[] trustedProxies) {
        if (trustedProxies != null) {
            this.trustedProxies = Arrays.stream(trustedProxies)
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.toSet());
        } else {
            this.trustedProxies = Collections.emptySet();
        }
    }

    public String resolveClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr() != null ? request.getRemoteAddr().trim() : "127.0.0.1";

        // Only trust X-Forwarded-For if request comes directly from a known trusted proxy
        if (trustedProxies.contains(remoteAddr)) {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isBlank()) {
                String[] ips = xForwardedFor.split(",");
                String clientIp = ips[0].trim();
                if (!clientIp.isBlank()) {
                    return clientIp;
                }
            }
        }

        return remoteAddr;
    }
}
