package org.netra.core.observability;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.netra.features.eligibility.dto.ErrorResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class NetraObservabilityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private NetraMetrics netraMetrics;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Observability: MeterRegistry registers and updates NetraMetrics business counters")
    void testNetraMetricsBusinessCounters() {
        double initialBloodReq = getCounterCount("netra.blood_requests.created.total", "urgency", "URGENT", "blood_group", "A_POSITIVE");
        netraMetrics.incrementBloodRequestsCreated("URGENT", "A_POSITIVE");
        assertThat(getCounterCount("netra.blood_requests.created.total", "urgency", "URGENT", "blood_group", "A_POSITIVE"))
                .isEqualTo(initialBloodReq + 1.0);

        double initialEmergency = getCounterCount("netra.emergency_requests.created.total");
        netraMetrics.incrementEmergencyRequestsCreated();
        assertThat(getCounterCount("netra.emergency_requests.created.total"))
                .isEqualTo(initialEmergency + 1.0);

        double initialMatches = getCounterCount("netra.donor_matches.created.total");
        netraMetrics.incrementDonorMatchesCreated(3);
        assertThat(getCounterCount("netra.donor_matches.created.total"))
                .isEqualTo(initialMatches + 3.0);

        double initialAccepted = getCounterCount("netra.donor_matches.response.total", "outcome", "ACCEPTED");
        netraMetrics.incrementDonorMatchResponse("ACCEPTED");
        assertThat(getCounterCount("netra.donor_matches.response.total", "outcome", "ACCEPTED"))
                .isEqualTo(initialAccepted + 1.0);

        double initialClaimed = getCounterCount("netra.donations.claimed.total", "source_type", "VOLUNTARY");
        netraMetrics.incrementDonationsClaimed("VOLUNTARY");
        assertThat(getCounterCount("netra.donations.claimed.total", "source_type", "VOLUNTARY"))
                .isEqualTo(initialClaimed + 1.0);

        double initialVerified = getCounterCount("netra.donations.verified.total");
        netraMetrics.incrementDonationsVerified();
        assertThat(getCounterCount("netra.donations.verified.total"))
                .isEqualTo(initialVerified + 1.0);

        double initialFulfilled = getCounterCount("netra.fulfillments.completed.total");
        netraMetrics.incrementFulfillmentsCompleted();
        assertThat(getCounterCount("netra.fulfillments.completed.total"))
                .isEqualTo(initialFulfilled + 1.0);
    }

    @Test
    @DisplayName("Observability: MeterRegistry records concurrency and contention metrics")
    void testContentionMetrics() {
        double initialLockConflict = getCounterCount("netra.concurrency.optimistic_lock_conflicts.total", "aggregate", "BloodRequest");
        netraMetrics.incrementOptimisticLockConflict("BloodRequest");
        assertThat(getCounterCount("netra.concurrency.optimistic_lock_conflicts.total", "aggregate", "BloodRequest"))
                .isEqualTo(initialLockConflict + 1.0);

        double initialFulfillmentContention = getCounterCount("netra.concurrency.fulfillment_contention.total");
        netraMetrics.incrementFulfillmentContention();
        assertThat(getCounterCount("netra.concurrency.fulfillment_contention.total"))
                .isEqualTo(initialFulfillmentContention + 1.0);

        double initialRateLimit = getCounterCount("netra.ratelimit.rejected.total", "endpoint_group", "/api/v1/auth/login");
        netraMetrics.incrementRateLimitRejected("/api/v1/auth/login");
        assertThat(getCounterCount("netra.ratelimit.rejected.total", "endpoint_group", "/api/v1/auth/login"))
                .isEqualTo(initialRateLimit + 1.0);
    }

    @Test
    @DisplayName("Observability: MeterRegistry records scheduler and matching timer telemetry")
    void testTimersAndSchedulerMetrics() {
        netraMetrics.recordMatchingDuration(35);
        assertThat(meterRegistry.get("netra.matching.duration").timer().count()).isGreaterThanOrEqualTo(1L);

        double initialJobExec = getCounterCount("netra.scheduled_jobs.executions.total", "job_name", "test_job", "outcome", "SUCCESS");
        netraMetrics.recordScheduledJobExecution("test_job", "SUCCESS", 20, 5);

        assertThat(getCounterCount("netra.scheduled_jobs.executions.total", "job_name", "test_job", "outcome", "SUCCESS"))
                .isEqualTo(initialJobExec + 1.0);
        assertThat(meterRegistry.get("netra.scheduled_jobs.duration").tag("job_name", "test_job").timer().count())
                .isGreaterThanOrEqualTo(1L);
        assertThat(getCounterCount("netra.scheduled_jobs.processed_items.total", "job_name", "test_job"))
                .isGreaterThanOrEqualTo(5.0);
    }

    @Test
    @DisplayName("Observability: ErrorResponse includes correlationId and CorrelationIdFilter propagates it")
    void testCorrelationIdInErrorResponse() throws Exception {
        String testCorrelationId = "test-corr-abc-987654";

        MvcResult result = mockMvc.perform(get("/api/v1/blood-requests/00000000-0000-0000-0000-000000000000")
                        .header(CorrelationIdFilter.CORRELATION_ID_HEADER, testCorrelationId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andReturn();

        String responseHeader = result.getResponse().getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
        assertThat(responseHeader).isEqualTo(testCorrelationId);

        String json = result.getResponse().getContentAsString();
        ErrorResponse errorResponse = objectMapper.readValue(json, ErrorResponse.class);
        assertThat(errorResponse.getCorrelationId()).isEqualTo(testCorrelationId);
    }

    @Test
    @DisplayName("Observability: HikariCP pool metrics are registered in MeterRegistry")
    void testHikariCpMetricsRegistered() {
        // Assert HikariCP pool metrics exist in MeterRegistry
        assertThat(meterRegistry.find("hikaricp.connections").meter()).isNotNull();
    }

    private double getCounterCount(String name, String... tags) {
        var search = meterRegistry.find(name);
        if (tags != null && tags.length >= 2) {
            for (int i = 0; i < tags.length; i += 2) {
                search = search.tag(tags[i], tags[i + 1]);
            }
        }
        var counter = search.counter();
        return counter != null ? counter.count() : 0.0;
    }
}
