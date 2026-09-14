package org.netra.features.eligibility;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.netra.core.security.JwtTokenProvider;
import org.netra.core.security.SecurityUtils;
import org.netra.features.eligibility.dto.AnswerItemDto;
import org.netra.features.eligibility.dto.SubmitAnswersRequest;
import org.netra.features.eligibility.entity.EligibilitySession;
import org.netra.features.eligibility.repository.EligibilitySessionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class EligibilityControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EligibilitySessionRepository sessionRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("IT 1: Full session lifecycle - start session, submit answers, evaluate eligibility, retrieve safe result")
    void testFullSessionLifecycle() throws Exception {
        MvcResult sessionResult = mockMvc.perform(post("/api/v1/eligibility/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sessionId").exists())
                .andExpect(jsonPath("$.capabilityToken").exists())
                .andExpect(jsonPath("$.ruleVersion", is("INDIA-NBTC-2026-01")))
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")))
                .andReturn();

        String sessionJson = sessionResult.getResponse().getContentAsString();
        String sessionIdStr = objectMapper.readTree(sessionJson).get("sessionId").asText();
        String capabilityToken = objectMapper.readTree(sessionJson).get("capabilityToken").asText();
        UUID sessionId = UUID.fromString(sessionIdStr);

        List<AnswerItemDto> answers = List.of(
                new AnswerItemDto("AGE", "28"),
                new AnswerItemDto("WEIGHT_KG", "70"),
                new AnswerItemDto("BIOLOGICAL_SEX", "MALE"),
                new AnswerItemDto("PREVIOUS_DONATION", "false"),
                new AnswerItemDto("CURRENTLY_FEELING_WELL", "true"),
                new AnswerItemDto("FEVER_OR_ILLNESS_14D", "false"),
                new AnswerItemDto("CURRENT_MEDICATION", "false"),
                new AnswerItemDto("TATTOO_OR_PIERCING_6M", "false"),
                new AnswerItemDto("MAJOR_SURGERY_12M", "false"),
                new AnswerItemDto("DENTAL_PROCEDURE_72H", "false"),
                new AnswerItemDto("CHRONIC_OR_CARDIAC_CONDITION", "false"),
                new AnswerItemDto("SLEEP_HOURS_LAST_NIGHT", "true"),
                new AnswerItemDto("MEAL_WITHIN_4_HOURS", "true"),
                new AnswerItemDto("HYDRATED_TODAY", "true")
        );
        SubmitAnswersRequest submitRequest = new SubmitAnswersRequest(answers);

        mockMvc.perform(post("/api/v1/eligibility/sessions/" + sessionId + "/answers")
                .header("X-Capability-Token", capabilityToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(submitRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SUCCESS")))
                .andExpect(jsonPath("$.savedCount", is(14)));

        mockMvc.perform(post("/api/v1/eligibility/sessions/" + sessionId + "/check")
                .header("X-Capability-Token", capabilityToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", is("LIKELY_ELIGIBLE")))
                .andExpect(jsonPath("$.disclaimer", containsString("Pre-screening result only")))
                .andExpect(jsonPath("$.nextActions", hasSize(greaterThan(0))));

        mockMvc.perform(get("/api/v1/eligibility/sessions/" + sessionId + "/result")
                .header("X-Capability-Token", capabilityToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", is("LIKELY_ELIGIBLE")));
    }

    @Test
    @DisplayName("IT 2: Anti-Tampering - client forged result is ignored and server enforces truth")
    void testAntiTamperingResultIgnored() throws Exception {
        EligibilitySession session = new EligibilitySession(null, "INDIA-NBTC-2026-01", Instant.now().plus(1, ChronoUnit.HOURS));
        String capabilityToken = SecurityUtils.generateSecureToken();
        session.setCapabilityTokenHash(SecurityUtils.sha256Hex(capabilityToken));
        session = sessionRepository.save(session);

        List<AnswerItemDto> answers = List.of(
                new AnswerItemDto("AGE", "22"),
                new AnswerItemDto("WEIGHT_KG", "40"),
                new AnswerItemDto("BIOLOGICAL_SEX", "FEMALE"),
                new AnswerItemDto("PREVIOUS_DONATION", "false"),
                new AnswerItemDto("CURRENTLY_FEELING_WELL", "true"),
                new AnswerItemDto("FEVER_OR_ILLNESS_14D", "false"),
                new AnswerItemDto("CURRENT_MEDICATION", "false"),
                new AnswerItemDto("TATTOO_OR_PIERCING_6M", "false"),
                new AnswerItemDto("MAJOR_SURGERY_12M", "false"),
                new AnswerItemDto("DENTAL_PROCEDURE_72H", "false"),
                new AnswerItemDto("CHRONIC_OR_CARDIAC_CONDITION", "false"),
                new AnswerItemDto("SLEEP_HOURS_LAST_NIGHT", "true"),
                new AnswerItemDto("MEAL_WITHIN_4_HOURS", "true")
        );
        SubmitAnswersRequest submitRequest = new SubmitAnswersRequest(answers);

        mockMvc.perform(post("/api/v1/eligibility/sessions/" + session.getId() + "/answers")
                .header("X-Capability-Token", capabilityToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(submitRequest)))
                .andExpect(status().isOk());

        String forgedPayload = "{\"result\": \"LIKELY_ELIGIBLE\", \"override\": true}";

        mockMvc.perform(post("/api/v1/eligibility/sessions/" + session.getId() + "/check")
                .header("X-Capability-Token", capabilityToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(forgedPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", is("TEMPORARY_DEFERRAL")))
                .andExpect(jsonPath("$.deferralReasons[0].code", is("WEIGHT_BELOW_MINIMUM")));
    }

    @Test
    @DisplayName("IT 3: IDOR / BOLA Prevention - User B cannot modify or access User A's session")
    void testIdorPrevention() throws Exception {
        UUID userA = UUID.randomUUID();
        UUID userB = UUID.randomUUID();

        String tokenUserA = jwtTokenProvider.generateAccessToken(userA, "ROLE_DONOR");
        String tokenUserB = jwtTokenProvider.generateAccessToken(userB, "ROLE_DONOR");

        MvcResult result = mockMvc.perform(post("/api/v1/eligibility/sessions")
                .header("Authorization", "Bearer " + tokenUserA)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isCreated())
                .andReturn();

        String sessionIdStr = objectMapper.readTree(result.getResponse().getContentAsString()).get("sessionId").asText();
        UUID sessionId = UUID.fromString(sessionIdStr);

        SubmitAnswersRequest attackRequest = new SubmitAnswersRequest(List.of(new AnswerItemDto("AGE", "25")));
        mockMvc.perform(post("/api/v1/eligibility/sessions/" + sessionId + "/answers")
                .header("Authorization", "Bearer " + tokenUserB)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(attackRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", is("FORBIDDEN")));
    }

    @Test
    @DisplayName("IT 4: Expired session rejection - accessing an expired session returns HTTP 410 Gone")
    void testExpiredSession() throws Exception {
        EligibilitySession session = new EligibilitySession(null, "INDIA-NBTC-2026-01", Instant.now().minus(10, ChronoUnit.MINUTES));
        session = sessionRepository.save(session);

        mockMvc.perform(post("/api/v1/eligibility/sessions/" + session.getId() + "/check")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.error", is("SESSION_EXPIRED")));
    }

    @Test
    @DisplayName("IT 5: Malformed JSON handling - returns safe 400 Bad Request without stack trace")
    void testMalformedJson() throws Exception {
        UUID randomId = UUID.randomUUID();
        mockMvc.perform(post("/api/v1/eligibility/sessions/" + randomId + "/answers")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ malformed json ::: }}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("MALFORMED_REQUEST")));
    }

    @Test
    @DisplayName("IT 6: Sensitive health data leak prevention - GET result does not leak raw health answers")
    void testSensitiveHealthDataNotLeakedInResult() throws Exception {
        EligibilitySession session = new EligibilitySession(null, "INDIA-NBTC-2026-01", Instant.now().plus(1, ChronoUnit.HOURS));
        String capabilityToken = SecurityUtils.generateSecureToken();
        session.setCapabilityTokenHash(SecurityUtils.sha256Hex(capabilityToken));
        session = sessionRepository.save(session);

        SubmitAnswersRequest submitRequest = new SubmitAnswersRequest(List.of(
                new AnswerItemDto("AGE", "26"),
                new AnswerItemDto("WEIGHT_KG", "65"),
                new AnswerItemDto("BIOLOGICAL_SEX", "MALE"),
                new AnswerItemDto("CURRENT_MEDICATION", "false"),
                new AnswerItemDto("PREVIOUS_DONATION", "false")
        ));

        mockMvc.perform(post("/api/v1/eligibility/sessions/" + session.getId() + "/answers")
                .header("X-Capability-Token", capabilityToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(submitRequest)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/eligibility/sessions/" + session.getId() + "/check")
                .header("X-Capability-Token", capabilityToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk());

        MvcResult result = mockMvc.perform(get("/api/v1/eligibility/sessions/" + session.getId() + "/result")
                .header("X-Capability-Token", capabilityToken))
                .andExpect(status().isOk())
                .andReturn();

        String content = result.getResponse().getContentAsString();
        assertFalse(content.contains("answers"), "Result DTO should not contain raw answers array");
    }

    @Test
    @DisplayName("IT 7: Capability token enforcement - anonymous session rejects missing or forged capability token")
    void testAnonymousSessionCapabilityTokenEnforcement() throws Exception {
        MvcResult sessionResult = mockMvc.perform(post("/api/v1/eligibility/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isCreated())
                .andReturn();

        String sessionJson = sessionResult.getResponse().getContentAsString();
        String sessionIdStr = objectMapper.readTree(sessionJson).get("sessionId").asText();
        String capabilityToken = objectMapper.readTree(sessionJson).get("capabilityToken").asText();
        UUID sessionId = UUID.fromString(sessionIdStr);

        SubmitAnswersRequest request = new SubmitAnswersRequest(List.of(new AnswerItemDto("AGE", "25")));

        // 1. Missing header -> 403
        mockMvc.perform(post("/api/v1/eligibility/sessions/" + sessionId + "/answers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", is("FORBIDDEN")));

        // 2. Forged header -> 403
        mockMvc.perform(post("/api/v1/eligibility/sessions/" + sessionId + "/answers")
                .header("X-Capability-Token", "forged-token-abc-1234567890")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", is("FORBIDDEN")));

        // 3. Valid header -> 200
        mockMvc.perform(post("/api/v1/eligibility/sessions/" + sessionId + "/answers")
                .header("X-Capability-Token", capabilityToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SUCCESS")));
    }

    @Test
    @DisplayName("IT 8: Cross-session capability token isolation - Token A cannot be used to access Session B")
    void testCrossSessionCapabilityTokenIsolation() throws Exception {
        MvcResult resA = mockMvc.perform(post("/api/v1/eligibility/sessions")
                .with(req -> { req.setRemoteAddr("127.0.8.1"); return req; })
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isCreated())
                .andReturn();
        String tokenA = objectMapper.readTree(resA.getResponse().getContentAsString()).get("capabilityToken").asText();

        MvcResult resB = mockMvc.perform(post("/api/v1/eligibility/sessions")
                .with(req -> { req.setRemoteAddr("127.0.8.2"); return req; })
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isCreated())
                .andReturn();
        String sessionBId = objectMapper.readTree(resB.getResponse().getContentAsString()).get("sessionId").asText();

        SubmitAnswersRequest request = new SubmitAnswersRequest(List.of(new AnswerItemDto("AGE", "25")));

        mockMvc.perform(post("/api/v1/eligibility/sessions/" + sessionBId + "/answers")
                .with(req -> { req.setRemoteAddr("127.0.8.2"); return req; })
                .header("X-Capability-Token", tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", is("FORBIDDEN")));
    }

    @Test
    @DisplayName("IT 9: Untrusted upstream cannot bypass rate limiting using spoofed X-Forwarded-For")
    void testUntrustedUpstreamCannotSpoofClientIp() throws Exception {
        boolean rateLimitTriggered = false;
        for (int i = 0; i < 25; i++) {
            var response = mockMvc.perform(post("/api/v1/eligibility/sessions")
                    .with(req -> {
                        req.setRemoteAddr("203.0.113.195");
                        return req;
                    })
                    .header("X-Forwarded-For", "198.51.100." + i)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}"))
                    .andReturn().getResponse();

            if (response.getStatus() == 429) {
                rateLimitTriggered = true;
                break;
            }
        }
        assertTrue(rateLimitTriggered, "Rate limiter should trigger 429 despite rotated X-Forwarded-For headers from untrusted direct peer");
    }

    @Test
    @DisplayName("IT 10: Numeric hours sleep evaluation - less than 4 hours triggers pre-donation preparation advisory")
    void testNumericSleepHoursEvaluation() throws Exception {
        MvcResult sessionResult = mockMvc.perform(post("/api/v1/eligibility/sessions")
                .with(req -> { req.setRemoteAddr("127.0.10.1"); return req; })
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isCreated())
                .andReturn();

        String sessionJson = sessionResult.getResponse().getContentAsString();
        String sessionId = objectMapper.readTree(sessionJson).get("sessionId").asText();
        String capabilityToken = objectMapper.readTree(sessionJson).get("capabilityToken").asText();

        List<AnswerItemDto> answers = List.of(
                new AnswerItemDto("AGE", "25"),
                new AnswerItemDto("WEIGHT_KG", "60"),
                new AnswerItemDto("BIOLOGICAL_SEX", "MALE"),
                new AnswerItemDto("PREVIOUS_DONATION", "false"),
                new AnswerItemDto("CURRENTLY_FEELING_WELL", "true"),
                new AnswerItemDto("FEVER_OR_ILLNESS_14D", "false"),
                new AnswerItemDto("CURRENT_MEDICATION", "false"),
                new AnswerItemDto("TATTOO_OR_PIERCING_6M", "false"),
                new AnswerItemDto("MAJOR_SURGERY_12M", "false"),
                new AnswerItemDto("DENTAL_PROCEDURE_72H", "false"),
                new AnswerItemDto("CHRONIC_OR_CARDIAC_CONDITION", "false"),
                new AnswerItemDto("SLEEP_HOURS_LAST_NIGHT", "3.5"),
                new AnswerItemDto("MEAL_WITHIN_4_HOURS", "true")
        );

        mockMvc.perform(post("/api/v1/eligibility/sessions/" + sessionId + "/answers")
                .with(req -> { req.setRemoteAddr("127.0.10.1"); return req; })
                .header("X-Capability-Token", capabilityToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new SubmitAnswersRequest(answers))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/eligibility/sessions/" + sessionId + "/check")
                .with(req -> { req.setRemoteAddr("127.0.10.1"); return req; })
                .header("X-Capability-Token", capabilityToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result", is("INSUFFICIENT_INFORMATION")))
                .andExpect(jsonPath("$.title", containsString("preparation needed")));
    }
}
