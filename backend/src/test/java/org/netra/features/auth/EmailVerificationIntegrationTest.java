package org.netra.features.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.netra.features.auth.dto.LoginRequest;
import org.netra.features.auth.dto.RegisterRequest;
import org.netra.features.auth.dto.ResendVerificationRequest;
import org.netra.features.auth.dto.VerifyEmailRequest;
import org.netra.features.auth.entity.EmailVerificationChallenge;
import org.netra.features.auth.repository.EmailVerificationChallengeRepository;
import org.netra.features.auth.service.DevConsoleVerificationProvider;
import org.netra.features.user.entity.User;
import org.netra.features.user.entity.UserStatus;
import org.netra.features.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "netra.security.auth.require-email-verification=true"
})
class EmailVerificationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailVerificationChallengeRepository challengeRepository;

    @BeforeEach
    void setUp() {
        DevConsoleVerificationProvider.clear();
    }

    private String uniquePhone() {
        return "+9198" + String.format("%08d", Math.abs(UUID.randomUUID().hashCode()) % 100_000_000L);
    }

    @Test
    @DisplayName("Verification 1: Registration sets UNVERIFIED status and blocks unverified login")
    void testRegistrationCreatesUnverifiedAccountAndBlocksLogin() throws Exception {
        String email = "unverified." + UUID.randomUUID() + "@netra.org";
        RegisterRequest registerReq = new RegisterRequest("Test Unverified", email, uniquePhone(), "StrongPass123");

        // 1. Register
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.status", is("UNVERIFIED")))
                .andExpect(jsonPath("$.accessToken").doesNotExist());

        // Verify DB status
        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        assertEquals(UserStatus.UNVERIFIED, user.getStatus());
        assertNull(user.getEmailVerifiedAt());

        // Verify challenge was recorded in sink
        String sentCode = DevConsoleVerificationProvider.getLastSentCode();
        assertNotNull(sentCode, "Verification code must be dispatched to provider");
        assertEquals(6, sentCode.length());

        // 2. Login before verification must be blocked
        LoginRequest loginReq = new LoginRequest(email, "StrongPass123");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", is("ACCOUNT_NOT_VERIFIED")));
    }

    @Test
    @DisplayName("Verification 2: Verify with correct code activates account and enables login")
    void testVerifyEmailLifecycle() throws Exception {
        String email = "verify." + UUID.randomUUID() + "@netra.org";
        RegisterRequest registerReq = new RegisterRequest("Test Verification", email, uniquePhone(), "StrongPass123");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        String validCode = DevConsoleVerificationProvider.getLastSentCode();
        assertNotNull(validCode);

        // 1. Invalid code attempt
        VerifyEmailRequest invalidReq = new VerifyEmailRequest(email, "000000");
        mockMvc.perform(post("/api/v1/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("VERIFICATION_FAILED")));

        // 2. Valid code verification
        VerifyEmailRequest validReq = new VerifyEmailRequest(email, validCode);
        mockMvc.perform(post("/api/v1/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", notNullValue()))
                .andExpect(jsonPath("$.user.status", is("ACTIVE")));

        // Verify DB state
        User verifiedUser = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        assertEquals(UserStatus.ACTIVE, verifiedUser.getStatus());
        assertNotNull(verifiedUser.getEmailVerifiedAt());

        EmailVerificationChallenge challenge = challengeRepository.findTopByEmailIgnoreCaseOrderByCreatedAtDesc(email).orElseThrow();
        assertNotNull(challenge.getUsedAt(), "Challenge must be marked used");

        // 3. Normal login succeeds now
        LoginRequest loginReq = new LoginRequest(email, "StrongPass123");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()));
    }

    @Test
    @DisplayName("Verification 3: Resend verification enforces rate limit cooldown")
    void testResendVerificationCooldown() throws Exception {
        String email = "resend." + UUID.randomUUID() + "@netra.org";
        RegisterRequest registerReq = new RegisterRequest("Test Resend", email, uniquePhone(), "StrongPass123");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        // Immediately request resend -> should be rate limited by 60s cooldown
        ResendVerificationRequest resendReq = new ResendVerificationRequest(email);
        mockMvc.perform(post("/api/v1/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resendReq)))
                .andExpect(status().isTooManyRequests());
    }
}
