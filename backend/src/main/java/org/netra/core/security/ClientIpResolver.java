package org.netra.core.security;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Secure client IP resolver.
 * Protects against rate-limiting bypass and IP spoofing via forged X-Forwarded-For headers.
 *
 * Security Invariants:
 * 1. CIDR Network Mask Evaluation: Supports IPv4/IPv6 single IPs and CIDR subnets
 *    (e.g., 127.0.0.1, ::1, 10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16, fe80::/10).
 * 2. Peer Authentication: X-Forwarded-For is only trusted if the immediate upstream peer (remoteAddr)
 *    matches a configured trusted proxy or trusted subnet.
 * 3. Reverse Traversal: Traverses X-Forwarded-For hops right-to-left (from closest trusted proxy backward).
 *    The first untrusted hop encountered is the authoritative client IP. Forged prefixes injected by clients
 *    are discarded.
 */
@Component
public class ClientIpResolver {

    private static final Logger log = LoggerFactory.getLogger(ClientIpResolver.class);

    private final List<IpAddressMatcher> trustedProxyMatchers;

    public ClientIpResolver(
            @Value("${netra.security.trusted-proxies:127.0.0.1,0:0:0:0:0:0:0:1,::1}") String[] trustedProxies) {
        if (trustedProxies != null) {
            this.trustedProxyMatchers = Arrays.stream(trustedProxies)
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .map(cidr -> {
                        try {
                            return new IpAddressMatcher(cidr);
                        } catch (Exception e) {
                            log.warn("Invalid trusted proxy IP/CIDR pattern ignored: '{}': {}", cidr, e.getMessage());
                            return null;
                        }
                    })
                    .filter(matcher -> matcher != null)
                    .collect(Collectors.toList());
        } else {
            this.trustedProxyMatchers = Collections.emptyList();
        }
    }

    public String resolveClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr() != null ? request.getRemoteAddr().trim() : "127.0.0.1";

        // Only trust X-Forwarded-For if request comes directly from a known trusted proxy / CIDR
        if (!isTrustedProxy(remoteAddr)) {
            return remoteAddr;
        }

        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor == null || xForwardedFor.isBlank()) {
            return remoteAddr;
        }

        String[] rawIps = xForwardedFor.split(",");
        List<String> ipList = Arrays.stream(rawIps)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());

        if (ipList.isEmpty()) {
            return remoteAddr;
        }

        // Walk backwards from the rightmost hop (closest to our trusted proxy).
        // The first untrusted IP encountered is the real client IP.
        for (int i = ipList.size() - 1; i >= 0; i--) {
            String hop = ipList.get(i);
            if (!isTrustedProxy(hop)) {
                return hop;
            }
        }

        // If all hops in X-Forwarded-For were trusted proxies, return the leftmost hop
        return ipList.get(0);
    }

    public boolean isTrustedProxy(String ip) {
        if (ip == null || ip.isBlank()) {
            return false;
        }
        for (IpAddressMatcher matcher : trustedProxyMatchers) {
            try {
                if (matcher.matches(ip)) {
                    return true;
                }
            } catch (Exception ignored) {
                // In case of malformed IP format, ignore and treat as untrusted
            }
        }
        return false;
    }
}
