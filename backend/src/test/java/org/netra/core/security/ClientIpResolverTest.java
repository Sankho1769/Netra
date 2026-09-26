package org.netra.core.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;

class ClientIpResolverTest {

    @Test
    @DisplayName("Untrusted peer: X-Forwarded-For is completely ignored when remoteAddr is not trusted")
    void testDirectConnectionWithoutTrustedProxy_IgnoresXForwardedFor() {
        ClientIpResolver resolver = new ClientIpResolver(new String[]{"127.0.0.1"});

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("203.0.113.50");
        request.addHeader("X-Forwarded-For", "8.8.8.8");

        assertEquals("203.0.113.50", resolver.resolveClientIp(request),
                "Resolver must ignore X-Forwarded-For if connection does not come from a trusted proxy");
    }

    @Test
    @DisplayName("Single trusted proxy: Resolves client IP from X-Forwarded-For")
    void testSingleTrustedProxy_ResolvesClientIp() {
        ClientIpResolver resolver = new ClientIpResolver(new String[]{"127.0.0.1"});

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("X-Forwarded-For", "203.0.113.195");

        assertEquals("203.0.113.195", resolver.resolveClientIp(request));
    }

    @Test
    @DisplayName("CIDR matching: Evaluates 10.0.0.0/8, 172.16.0.0/12, and 192.168.0.0/16 subnets")
    void testSubnetCidrEvaluation() {
        ClientIpResolver resolver = new ClientIpResolver(new String[]{
                "10.0.0.0/8", "172.16.0.0/12", "192.168.0.0/16"
        });

        // 10.x.x.x
        MockHttpServletRequest req1 = new MockHttpServletRequest();
        req1.setRemoteAddr("10.50.12.3");
        req1.addHeader("X-Forwarded-For", "198.51.100.10");
        assertEquals("198.51.100.10", resolver.resolveClientIp(req1));

        // 172.20.x.x (in 172.16.0.0/12)
        MockHttpServletRequest req2 = new MockHttpServletRequest();
        req2.setRemoteAddr("172.20.4.88");
        req2.addHeader("X-Forwarded-For", "198.51.100.20");
        assertEquals("198.51.100.20", resolver.resolveClientIp(req2));

        // 192.168.x.x
        MockHttpServletRequest req3 = new MockHttpServletRequest();
        req3.setRemoteAddr("192.168.1.1");
        req3.addHeader("X-Forwarded-For", "198.51.100.30");
        assertEquals("198.51.100.30", resolver.resolveClientIp(req3));

        // Out-of-subnet IP (e.g. 172.32.0.1 is not in /12)
        MockHttpServletRequest req4 = new MockHttpServletRequest();
        req4.setRemoteAddr("172.32.0.1");
        req4.addHeader("X-Forwarded-For", "1.1.1.1");
        assertEquals("172.32.0.1", resolver.resolveClientIp(req4),
                "IP outside CIDR subnet must not be treated as trusted proxy");
    }

    @Test
    @DisplayName("Anti-spoofing: Reverse traversal rejects client-injected spoofed headers")
    void testReverseTraversalRejectsSpoofedPrefix() {
        ClientIpResolver resolver = new ClientIpResolver(new String[]{"10.0.0.0/8"});

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1"); // Load balancer
        // Client at 198.51.100.5 sent X-Forwarded-For: 8.8.8.8. Load balancer appended 198.51.100.5.
        request.addHeader("X-Forwarded-For", "8.8.8.8, 198.51.100.5");

        assertEquals("198.51.100.5", resolver.resolveClientIp(request),
                "Resolver must pick the rightmost untrusted hop to eliminate spoofed prefixes");
    }

    @Test
    @DisplayName("Multi-hop trusted proxies: Skips internal trusted hops backwards")
    void testMultiHopTrustedProxies() {
        ClientIpResolver resolver = new ClientIpResolver(new String[]{"10.0.0.0/8", "172.16.0.0/12"});

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1"); // Internal ingress
        // CDN / Proxy chain: real client -> CDN (172.16.2.5) -> Ingress (10.0.0.1)
        request.addHeader("X-Forwarded-For", "198.51.100.77, 172.16.2.5");

        assertEquals("198.51.100.77", resolver.resolveClientIp(request));
    }

    @Test
    @DisplayName("IPv6: Evaluates IPv6 loopback and CIDR subnets")
    void testIpv6Evaluation() {
        ClientIpResolver resolver = new ClientIpResolver(new String[]{
                "::1", "0:0:0:0:0:0:0:1", "2001:db8::/32"
        });

        MockHttpServletRequest req1 = new MockHttpServletRequest();
        req1.setRemoteAddr("0:0:0:0:0:0:0:1");
        req1.addHeader("X-Forwarded-For", "2001:db8::cafe");
        assertEquals("2001:db8::cafe", resolver.resolveClientIp(req1));

        MockHttpServletRequest req2 = new MockHttpServletRequest();
        req2.setRemoteAddr("2001:db8:1234::1");
        req2.addHeader("X-Forwarded-For", "203.0.113.99");
        assertEquals("203.0.113.99", resolver.resolveClientIp(req2));
    }

    @Test
    @DisplayName("Edge cases: Null, empty, or blank X-Forwarded-For falls back to remoteAddr")
    void testEmptyHeaderFallback() {
        ClientIpResolver resolver = new ClientIpResolver(new String[]{"127.0.0.1"});

        MockHttpServletRequest req1 = new MockHttpServletRequest();
        req1.setRemoteAddr("127.0.0.1");
        assertEquals("127.0.0.1", resolver.resolveClientIp(req1));

        MockHttpServletRequest req2 = new MockHttpServletRequest();
        req2.setRemoteAddr("127.0.0.1");
        req2.addHeader("X-Forwarded-For", "   ");
        assertEquals("127.0.0.1", resolver.resolveClientIp(req2));
    }
}
