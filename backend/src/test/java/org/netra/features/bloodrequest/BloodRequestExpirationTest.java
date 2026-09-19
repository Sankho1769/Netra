package org.netra.features.bloodrequest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.netra.features.bloodrequest.repository.BloodRequestRepository;
import org.netra.features.bloodrequest.service.BloodRequestExpirationService;
import org.netra.features.matching.service.DonorMatchLifecycleService;

import java.lang.reflect.Proxy;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BloodRequestExpirationTest {

    static class StubDonorMatchLifecycleService extends DonorMatchLifecycleService {
        final List<UUID> expiredRequestIds = new ArrayList<>();

        StubDonorMatchLifecycleService() {
            super(null, null, Clock.systemUTC());
        }

        @Override
        public int expireActiveMatchesForRequest(UUID requestId) {
            expiredRequestIds.add(requestId);
            return 1;
        }
    }

    @Test
    @DisplayName("Unit: processExpirations uses injected Clock when referenceTime is null")
    void testProcessExpirations_UsesInjectedClock() {
        Instant fixedNow = Instant.parse("2026-09-19T12:00:00Z");
        Clock fixedClock = Clock.fixed(fixedNow, ZoneOffset.UTC);

        AtomicReference<Instant> evaluatedInstant = new AtomicReference<>();
        UUID overdueId = UUID.randomUUID();

        BloodRequestRepository repo = (BloodRequestRepository) Proxy.newProxyInstance(
                BloodRequestRepository.class.getClassLoader(),
                new Class<?>[]{BloodRequestRepository.class},
                (proxy, method, args) -> {
                    if ("findOverdueRequestIds".equals(method.getName())) {
                        evaluatedInstant.set((Instant) args[0]);
                        return List.of(overdueId);
                    }
                    if ("expireDueRequests".equals(method.getName())) {
                        return 1;
                    }
                    return null;
                }
        );

        StubDonorMatchLifecycleService stubLifecycle = new StubDonorMatchLifecycleService();
        BloodRequestExpirationService service =
                new BloodRequestExpirationService(repo, stubLifecycle, fixedClock);

        int expiredCount = service.processExpirations(null);

        assertEquals(1, expiredCount);
        assertEquals(fixedNow, evaluatedInstant.get(), "Must evaluate against injected clock instant");
        assertEquals(List.of(overdueId), stubLifecycle.expiredRequestIds, "Must cascade match expiration for overdue request");
    }

    @Test
    @DisplayName("Unit: scheduledExpiration delegates to processExpirations with Clock instant")
    void testScheduledExpiration_UsesClockInstant() {
        Instant fixedNow = Instant.parse("2026-09-19T14:30:00Z");
        Clock fixedClock = Clock.fixed(fixedNow, ZoneOffset.UTC);

        AtomicReference<Instant> evaluatedInstant = new AtomicReference<>();

        BloodRequestRepository repo = (BloodRequestRepository) Proxy.newProxyInstance(
                BloodRequestRepository.class.getClassLoader(),
                new Class<?>[]{BloodRequestRepository.class},
                (proxy, method, args) -> {
                    if ("findOverdueRequestIds".equals(method.getName())) {
                        evaluatedInstant.set((Instant) args[0]);
                        return List.of();
                    }
                    return null;
                }
        );

        StubDonorMatchLifecycleService stubLifecycle = new StubDonorMatchLifecycleService();
        BloodRequestExpirationService service =
                new BloodRequestExpirationService(repo, stubLifecycle, fixedClock);

        service.scheduledExpiration();

        assertEquals(fixedNow, evaluatedInstant.get(), "Scheduled expiration must evaluate against injected clock instant");
    }

    @Test
    @DisplayName("Unit: Both BloodRequestExpirationService and DonorMatchLifecycleService share the same injected Clock deterministically")
    void testSharedClockInjectionDeterminism() {
        Instant fixedInstant = Instant.parse("2026-09-19T10:00:00Z");
        Clock sharedClock = Clock.fixed(fixedInstant, ZoneOffset.UTC);

        // Verify BloodRequestExpirationService evaluates at sharedClock instant
        AtomicReference<Instant> reqEvalTime = new AtomicReference<>();
        BloodRequestRepository repo = (BloodRequestRepository) Proxy.newProxyInstance(
                BloodRequestRepository.class.getClassLoader(),
                new Class<?>[]{BloodRequestRepository.class},
                (proxy, method, args) -> {
                    if ("findOverdueRequestIds".equals(method.getName())) {
                        reqEvalTime.set((Instant) args[0]);
                        return List.of();
                    }
                    return null;
                }
        );
        BloodRequestExpirationService bloodReqService =
                new BloodRequestExpirationService(repo, null, sharedClock);
        bloodReqService.scheduledExpiration();
        assertEquals(fixedInstant, reqEvalTime.get(), "BloodRequestExpirationService must evaluate at sharedClock instant");

        // Verify DonorMatchLifecycleService evaluates at the exact same sharedClock instant
        AtomicReference<Instant> matchEvalTime = new AtomicReference<>();
        org.netra.features.matching.repository.DonorMatchRepository matchRepo = (org.netra.features.matching.repository.DonorMatchRepository) Proxy.newProxyInstance(
                org.netra.features.matching.repository.DonorMatchRepository.class.getClassLoader(),
                new Class<?>[]{org.netra.features.matching.repository.DonorMatchRepository.class},
                (proxy, method, args) -> {
                    if ("expireOverdueMatches".equals(method.getName())) {
                        matchEvalTime.set((Instant) args[0]);
                        return 0;
                    }
                    return null;
                }
        );
        DonorMatchLifecycleService matchLifecycleService =
                new DonorMatchLifecycleService(matchRepo, null, sharedClock);
        matchLifecycleService.scheduledExpiration();
        assertEquals(fixedInstant, matchEvalTime.get(), "DonorMatchLifecycleService must evaluate at same sharedClock instant");
    }
}
