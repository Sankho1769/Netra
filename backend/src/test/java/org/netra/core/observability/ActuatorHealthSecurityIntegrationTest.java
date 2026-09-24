package org.netra.core.observability;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.netra.core.security.JwtTokenProvider;
import org.netra.features.user.entity.User;
import org.netra.features.user.entity.UserRole;
import org.netra.features.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ActuatorHealthSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private UserRepository userRepository;

    private User adminUser;
    private User donorUser;

    @BeforeEach
    void setUp() {
        donorUser = userRepository.save(new User(
                "Test Donor",
                "donor." + UUID.randomUUID() + "@netra.org",
                "+919876543210",
                "hashedPass",
                Set.of(UserRole.ROLE_DONOR)
        ));

        adminUser = userRepository.save(new User(
                "Test Admin",
                "admin." + UUID.randomUUID() + "@netra.org",
                "+919876543211",
                "hashedPass",
                Set.of(UserRole.ROLE_ADMIN)
        ));
    }

    @Test
    @DisplayName("Public Actuator: /actuator/health is accessible without authentication and returns UP")
    void testPublicHealthEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/health")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")))
                .andExpect(header().exists(CorrelationIdFilter.CORRELATION_ID_HEADER));
    }

    @Test
    @DisplayName("Public Actuator: /actuator/health/liveness probe returns UP")
    void testPublicLivenessProbe() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")));
    }

    @Test
    @DisplayName("Public Actuator: /actuator/health/readiness probe returns UP")
    void testPublicReadinessProbe() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")));
    }

    @Test
    @DisplayName("Secured Actuator: /actuator/metrics is unauthorized without authentication")
    void testMetricsEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/metrics")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Secured Actuator: /actuator/metrics is forbidden for non-ADMIN users")
    void testMetricsEndpointForbiddenForDonor() throws Exception {
        String donorToken = jwtTokenProvider.generateAccessToken(donorUser.getId(), List.of("ROLE_DONOR"));

        mockMvc.perform(get("/actuator/metrics")
                        .header("Authorization", "Bearer " + donorToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Secured Actuator: /actuator/metrics is accessible for ADMIN users")
    void testMetricsEndpointAccessibleForAdmin() throws Exception {
        String adminToken = jwtTokenProvider.generateAccessToken(adminUser.getId(), List.of("ROLE_ADMIN"));

        mockMvc.perform(get("/actuator/metrics")
                        .header("Authorization", "Bearer " + adminToken)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.names", hasItem("jvm.memory.used")));
    }
}
