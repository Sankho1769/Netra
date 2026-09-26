package org.netra.features.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.netra.core.security.JwtTokenProvider;
import org.netra.core.security.SecurityUtils;
import org.netra.features.auth.dto.LoginRequest;
import org.netra.features.auth.dto.LogoutRequest;
import org.netra.features.auth.dto.RegisterRequest;
import org.netra.features.auth.dto.TokenRefreshRequest;
import org.netra.features.user.entity.RefreshSession;
import org.netra.features.user.entity.User;
import org.netra.features.user.entity.UserRole;
import org.netra.features.user.entity.UserStatus;
import org.netra.features.user.repository.RefreshSessionRepository;
import org.netra.features.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshSessionRepository refreshSessionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String uniquePhone() {
        return "+919" + String.format("%09d", Math.abs(UUID.randomUUID().hashCode()) % 1_000_000_000L);
    }

    @Test
    @DisplayName("Auth 1 & 2: Registration success & password hashing verification")
    void testRegistrationSuccessAndPasswordHashing() throws Exception {
        String email = "donor." + UUID.randomUUID() + "@netra.org";
        RegisterRequest request = new RegisterRequest("Jolly Banerjee", email, uniquePhone(), "SafePass123");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                .with(req -> { req.setRemoteAddr("127.1.1.1"); return req; })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", notNullValue()))
                .andExpect(jsonPath("$.user.email", is(email)))
                .andExpect(jsonPath("$.user.roles", hasItem("ROLE_DONOR")))
                .andReturn();

        // Verify password is NOT stored in plaintext
        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        assertNotEquals("SafePass123", user.getPasswordHash());
        assertTrue(passwordEncoder.matches("SafePass123", user.getPasswordHash()));
    }

    @Test
    @DisplayName("Auth 3: Client role escalation attempt is blocked during registration")
    void testRoleEscalationBlocked() throws Exception {
        String email = "hacker." + UUID.randomUUID() + "@netra.org";
        String phone = uniquePhone();
        // Attempt to pass "role": "ROLE_ADMIN" or "roles": ["ROLE_ADMIN"] in the payload
        String forgedPayload = "{\"fullName\":\"Attacker\",\"email\":\"" + email + "\",\"phone\":\"" + phone + "\",\"password\":\"HackerPass123\",\"role\":\"ROLE_ADMIN\",\"roles\":[\"ROLE_ADMIN\"]}";

        mockMvc.perform(post("/api/v1/auth/register")
                .with(req -> { req.setRemoteAddr("127.1.1.2"); return req; })
                .contentType(MediaType.APPLICATION_JSON)
                .content(forgedPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.roles", not(hasItem("ROLE_ADMIN"))))
                .andExpect(jsonPath("$.user.roles", hasItem("ROLE_DONOR")));

        User user = userRepository.findByEmailIgnoreCase(email).orElseThrow();
        assertFalse(user.getRoles().contains(UserRole.ROLE_ADMIN), "Server must not assign ROLE_ADMIN from client input");
    }

    @Test
    @DisplayName("Auth 4: Duplicate email registration handled securely without SQL leak")
    void testDuplicateEmailRegistration() throws Exception {
        String email = "duplicate." + UUID.randomUUID() + "@netra.org";
        RegisterRequest request = new RegisterRequest("Jolly Banerjee", email, uniquePhone(), "SafePass123");

        mockMvc.perform(post("/api/v1/auth/register")
                .with(req -> { req.setRemoteAddr("127.1.1.3"); return req; })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Second registration with same email
        mockMvc.perform(post("/api/v1/auth/register")
                .with(req -> { req.setRemoteAddr("127.1.1.3"); return req; })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("REGISTRATION_FAILED")));
    }

    @Test
    @DisplayName("Auth 4b: Duplicate mobile registration rejected securely")
    void testDuplicatePhoneRegistration() throws Exception {
        String phone = uniquePhone();
        String email1 = "donor1." + UUID.randomUUID() + "@netra.org";
        String email2 = "donor2." + UUID.randomUUID() + "@netra.org";

        RegisterRequest request1 = new RegisterRequest("Jolly Banerjee", email1, phone, "SafePass123");
        RegisterRequest request2 = new RegisterRequest("Ram Krishna Banerjee", email2, phone, "SafePass123");

        mockMvc.perform(post("/api/v1/auth/register")
                .with(req -> { req.setRemoteAddr("127.1.1.31"); return req; })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/register")
                .with(req -> { req.setRemoteAddr("127.1.1.32"); return req; })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("REGISTRATION_FAILED")));
    }

    @Test
    @DisplayName("Auth 4c: Invalid or missing phone number registration rejected")
    void testInvalidPhoneRegistration() throws Exception {
        String email = "invalidphone." + UUID.randomUUID() + "@netra.org";
        // Invalid 6 digit phone
        RegisterRequest invalidPhoneReq = new RegisterRequest("Invalid Phone", email, "123456", "SafePass123");

        mockMvc.perform(post("/api/v1/auth/register")
                .with(req -> { req.setRemoteAddr("127.1.1.33"); return req; })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidPhoneReq)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Auth 5: Weak password policy rejection")
    void testWeakPasswordRejected() throws Exception {
        // Less than 8 chars
        RegisterRequest shortPass = new RegisterRequest("Weak User", "weak1@netra.org", uniquePhone(), "Short1");
        mockMvc.perform(post("/api/v1/auth/register")
                .with(req -> { req.setRemoteAddr("127.1.1.4"); return req; })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(shortPass)))
                .andExpect(status().isBadRequest());

        // Missing digit
        RegisterRequest noDigit = new RegisterRequest("Weak User", "weak2@netra.org", uniquePhone(), "NoDigitHere");
        mockMvc.perform(post("/api/v1/auth/register")
                .with(req -> { req.setRemoteAddr("127.1.1.4"); return req; })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(noDigit)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Auth 6, 7 & 8: Login success, wrong password, and unknown account generic error")
    void testLoginScenarios() throws Exception {
        String email = "login." + UUID.randomUUID() + "@netra.org";
        RegisterRequest reg = new RegisterRequest("Login Donor", email, uniquePhone(), "CorrectPass123");
        mockMvc.perform(post("/api/v1/auth/register")
                .with(req -> { req.setRemoteAddr("127.1.1.5"); return req; })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        // 1. Successful login
        LoginRequest validLogin = new LoginRequest(email, "CorrectPass123");
        mockMvc.perform(post("/api/v1/auth/login")
                .with(req -> { req.setRemoteAddr("127.1.1.5"); return req; })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", notNullValue()));

        // 2. Wrong password -> generic error
        LoginRequest wrongPass = new LoginRequest(email, "WrongPass999");
        mockMvc.perform(post("/api/v1/auth/login")
                .with(req -> { req.setRemoteAddr("127.1.1.5"); return req; })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(wrongPass)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("Invalid email or password")));

        // 3. Unknown email -> identical generic error (anti-enumeration)
        LoginRequest unknownEmail = new LoginRequest("nonexistent@netra.org", "SomePass123");
        mockMvc.perform(post("/api/v1/auth/login")
                .with(req -> { req.setRemoteAddr("127.1.1.5"); return req; })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(unknownEmail)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("Invalid email or password")));
    }

    @Test
    @DisplayName("Auth 9 & 10: Suspended and deactivated accounts are blocked")
    void testSuspendedAndDeactivatedUsersBlocked() throws Exception {
        // Create suspended user
        String suspendedEmail = "suspended." + UUID.randomUUID() + "@netra.org";
        String suspendedPhone = "+91987" + String.format("%07d", Math.abs((long) suspendedEmail.hashCode()) % 10_000_000L);
        User suspended = new User("Suspended User", suspendedEmail, suspendedPhone, passwordEncoder.encode("Pass12345"), Set.of(UserRole.ROLE_DONOR));
        suspended.setStatus(UserStatus.SUSPENDED);
        suspended = userRepository.save(suspended);

        LoginRequest req1 = new LoginRequest(suspended.getEmail(), "Pass12345");
        mockMvc.perform(post("/api/v1/auth/login")
                .with(req -> { req.setRemoteAddr("127.1.1.6"); return req; })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req1)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", is("ACCOUNT_DISABLED")));

        // Even with valid access token, suspended user is blocked by JwtAuthenticationFilter
        String token = jwtTokenProvider.generateAccessToken(suspended.getId(), "ROLE_DONOR");
        mockMvc.perform(get("/api/v1/auth/me")
                .with(req -> { req.setRemoteAddr("127.1.1.6"); return req; })
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());

        // Create deactivated user
        String deactivatedEmail = "deactivated." + UUID.randomUUID() + "@netra.org";
        String deactivatedPhone = "+91987" + String.format("%07d", Math.abs((long) deactivatedEmail.hashCode()) % 10_000_000L);
        User deactivated = new User("Deactivated User", deactivatedEmail, deactivatedPhone, passwordEncoder.encode("Pass12345"), Set.of(UserRole.ROLE_DONOR));
        deactivated.setStatus(UserStatus.DEACTIVATED);
        deactivated = userRepository.save(deactivated);

        LoginRequest req2 = new LoginRequest(deactivated.getEmail(), "Pass12345");
        mockMvc.perform(post("/api/v1/auth/login")
                .with(req -> { req.setRemoteAddr("127.1.1.6"); return req; })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req2)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", is("ACCOUNT_DISABLED")));
    }

    @Test
    @DisplayName("Auth 11, 12 & 13: Access token verification, expiry, and REFRESH token as ACCESS token rejection")
    void testAccessTokenValidationAndRejection() throws Exception {
        String activeEmail = "active." + UUID.randomUUID() + "@netra.org";
        String activePhone = "+91987" + String.format("%07d", Math.abs((long) activeEmail.hashCode()) % 10_000_000L);
        User user = new User("Active User", activeEmail, activePhone, passwordEncoder.encode("Pass12345"), Set.of(UserRole.ROLE_DONOR));
        user = userRepository.save(user);

        // 1. Valid access token -> 200 on /me
        String validToken = jwtTokenProvider.generateAccessToken(user.getId(), "ROLE_DONOR");
        mockMvc.perform(get("/api/v1/auth/me")
                .with(req -> { req.setRemoteAddr("127.1.1.7"); return req; })
                .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is(user.getEmail())));

        // 2. REFRESH token presented as Bearer access token -> 401 Unauthorized
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());
        mockMvc.perform(get("/api/v1/auth/me")
                .with(req -> { req.setRemoteAddr("127.1.1.7"); return req; })
                .header("Authorization", "Bearer " + refreshToken))
                .andExpect(status().isUnauthorized());

        // 3. Tampered JWT token -> 401 Unauthorized
        String tamperedToken = validToken.substring(0, validToken.length() - 6) + "abcdef";
        mockMvc.perform(get("/api/v1/auth/me")
                .with(req -> { req.setRemoteAddr("127.1.1.7"); return req; })
                .header("Authorization", "Bearer " + tamperedToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Auth 14, 15, 16 & 17: Refresh rotation, old token rejection, and reuse anomaly detection with family revocation")
    void testRefreshRotationAndReuseAnomalyDetection() throws Exception {
        String email = "rotation." + UUID.randomUUID() + "@netra.org";
        RegisterRequest reg = new RegisterRequest("Rotate User", email, uniquePhone(), "RotatePass123");
        MvcResult res = mockMvc.perform(post("/api/v1/auth/register")
                .with(req -> { req.setRemoteAddr("127.1.1.8"); return req; })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(res.getResponse().getContentAsString());
        String initialRefreshToken = json.get("refreshToken").asText();

        // 1. First refresh -> succeeds, rotates token
        TokenRefreshRequest refreshReq1 = new TokenRefreshRequest(initialRefreshToken);
        MvcResult refreshRes1 = mockMvc.perform(post("/api/v1/auth/refresh")
                .with(req -> { req.setRemoteAddr("127.1.1.8"); return req; })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshReq1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", notNullValue()))
                .andReturn();

        String rotatedRefreshToken = objectMapper.readTree(refreshRes1.getResponse().getContentAsString()).get("refreshToken").asText();
        assertNotEquals(initialRefreshToken, rotatedRefreshToken, "New refresh token must be issued");

        // 2. Reuse old rotated token -> triggers REUSE ANOMALY DETECTION!
        mockMvc.perform(post("/api/v1/auth/refresh")
                .with(req -> { req.setRemoteAddr("127.1.1.8"); return req; })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshReq1)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error", is("REFRESH_TOKEN_REUSED")));

        // 3. Family revocation verification: The child rotated token must NOW ALSO be revoked!
        TokenRefreshRequest refreshReq2 = new TokenRefreshRequest(rotatedRefreshToken);
        mockMvc.perform(post("/api/v1/auth/refresh")
                .with(req -> { req.setRemoteAddr("127.1.1.8"); return req; })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshReq2)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Auth 18 & 19: Logout and logout-all invalidates refresh sessions")
    void testLogoutAndLogoutAll() throws Exception {
        String email = "logout." + UUID.randomUUID() + "@netra.org";
        RegisterRequest reg = new RegisterRequest("Logout User", email, uniquePhone(), "LogoutPass123");
        MvcResult regResult = mockMvc.perform(post("/api/v1/auth/register")
                .with(req -> { req.setRemoteAddr("127.1.1.9"); return req; })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(regResult.getResponse().getContentAsString());
        String accessToken = json.get("accessToken").asText();
        String refreshToken = json.get("refreshToken").asText();

        // 1. Logout single session
        mockMvc.perform(post("/api/v1/auth/logout")
                .with(req -> { req.setRemoteAddr("127.1.1.9"); return req; })
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LogoutRequest(refreshToken))))
                .andExpect(status().isOk());

        // Trying to refresh with logged out token -> fails
        mockMvc.perform(post("/api/v1/auth/refresh")
                .with(req -> { req.setRemoteAddr("127.1.1.9"); return req; })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new TokenRefreshRequest(refreshToken))))
                .andExpect(status().isUnauthorized());

        // 2. Login again and test logout-all
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .with(req -> { req.setRemoteAddr("127.1.1.9"); return req; })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(email, "LogoutPass123"))))
                .andExpect(status().isOk())
                .andReturn();

        String newAccess = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("accessToken").asText();
        String newRefresh = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("refreshToken").asText();

        mockMvc.perform(post("/api/v1/auth/logout-all")
                .with(req -> { req.setRemoteAddr("127.1.1.9"); return req; })
                .header("Authorization", "Bearer " + newAccess))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/refresh")
                .with(req -> { req.setRemoteAddr("127.1.1.9"); return req; })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new TokenRefreshRequest(newRefresh))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Auth 20 & 21: IDOR/BOLA Protection on /api/v1/users/{id}")
    void testIdorProfileProtection() throws Exception {
        User userA = new User("User Alpha", "alpha." + UUID.randomUUID() + "@netra.org", "+919876543220", passwordEncoder.encode("Pass12345"), Set.of(UserRole.ROLE_DONOR));
        userA = userRepository.save(userA);

        User userB = new User("User Beta", "beta." + UUID.randomUUID() + "@netra.org", "+919876543221", passwordEncoder.encode("Pass12345"), Set.of(UserRole.ROLE_DONOR));
        userB = userRepository.save(userB);

        User admin = new User("System Admin", "admin." + UUID.randomUUID() + "@netra.org", "+919876543222", passwordEncoder.encode("Pass12345"), Set.of(UserRole.ROLE_ADMIN));
        admin = userRepository.save(admin);

        String tokenA = jwtTokenProvider.generateAccessToken(userA.getId(), "ROLE_DONOR");
        String tokenAdmin = jwtTokenProvider.generateAccessToken(admin.getId(), "ROLE_ADMIN");

        // 1. User A accesses own profile -> 200 OK
        mockMvc.perform(get("/api/v1/users/" + userA.getId())
                .with(req -> { req.setRemoteAddr("127.1.1.10"); return req; })
                .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is(userA.getEmail())));

        // 2. User A attempts to access User B's profile -> 403 Forbidden (IDOR Blocked!)
        mockMvc.perform(get("/api/v1/users/" + userB.getId())
                .with(req -> { req.setRemoteAddr("127.1.1.10"); return req; })
                .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error", is("FORBIDDEN")));

        // 3. Admin accesses User B's profile -> 200 OK
        mockMvc.perform(get("/api/v1/users/" + userB.getId())
                .with(req -> { req.setRemoteAddr("127.1.1.10"); return req; })
                .header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is(userB.getEmail())));
    }

    @Test
    @DisplayName("Auth 22: Rate limiting on authentication endpoints")
    void testAuthRateLimiting() throws Exception {
        boolean rateLimitTriggered = false;
        LoginRequest req = new LoginRequest("ratelimit@netra.org", "SomePassword123");

        for (int i = 0; i < 25; i++) {
            var response = mockMvc.perform(post("/api/v1/auth/login")
                    .with(r -> {
                        r.setRemoteAddr("203.0.113.88");
                        return r;
                    })
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(req)))
                    .andReturn().getResponse();

            if (response.getStatus() == 429) {
                rateLimitTriggered = true;
                break;
            }
        }
        assertTrue(rateLimitTriggered, "Exceeding 20 requests/minute on login must trigger HTTP 429");
    }

    @Test
    @DisplayName("Auth 24: Sensitive health & security information not leaked in profile DTO")
    void testSensitiveDataNotLeakedInProfile() throws Exception {
        User user = new User("Safe Donor", "safe." + UUID.randomUUID() + "@netra.org", "+919876543299", passwordEncoder.encode("Pass12345"), Set.of(UserRole.ROLE_DONOR));
        user = userRepository.save(user);

        String token = jwtTokenProvider.generateAccessToken(user.getId(), "ROLE_DONOR");
        MvcResult res = mockMvc.perform(get("/api/v1/users/" + user.getId())
                .with(req -> { req.setRemoteAddr("127.1.1.12"); return req; })
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        String body = res.getResponse().getContentAsString();
        assertFalse(body.contains("password"), "Response must not contain password field");
        assertFalse(body.contains("tokenHash"), "Response must not contain tokenHash field");
        assertFalse(body.contains("refresh"), "Response must not contain refresh token field");
        assertFalse(body.contains("medical"), "Response must not contain medical field");
    }
}
