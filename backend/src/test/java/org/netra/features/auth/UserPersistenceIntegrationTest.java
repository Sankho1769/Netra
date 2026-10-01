package org.netra.features.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.netra.features.auth.dto.LoginRequest;
import org.netra.features.auth.dto.LogoutRequest;
import org.netra.features.auth.dto.RegisterRequest;
import org.netra.features.user.entity.User;
import org.netra.features.user.entity.UserRole;
import org.netra.features.user.entity.UserStatus;
import org.netra.features.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Optional;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Production Regression Tests for NETRA User Account Persistence,
 * Password Hashing Integrity, Session Lifecycle, and Restart Survival.
 */
@SpringBootTest
@AutoConfigureMockMvc
class UserPersistenceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String uniquePhone() {
        return "+919" + String.format("%09d", Math.abs(UUID.randomUUID().hashCode()) % 1_000_000_000L);
    }

    @Test
    @DisplayName("Persistence 1: User registration commits user row to database with BCrypt hash and without plaintext password")
    void testUserRegistrationPersistsCommittedRowWithBCryptHash() throws Exception {
        String email = "persistent.donor." + UUID.randomUUID() + "@netra.org";
        String rawPassword = "StrongPassword123";
        RegisterRequest request = new RegisterRequest("Sunita Sharma", email, uniquePhone(), rawPassword);

        mockMvc.perform(post("/api/v1/auth/register")
                .with(req -> { req.setRemoteAddr("127.1.2.1"); return req; })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", notNullValue()))
                .andExpect(jsonPath("$.user.email", is(email)));

        // Verify committed row in database
        Optional<User> optionalUser = userRepository.findByEmailIgnoreCase(email);
        assertTrue(optionalUser.isPresent(), "User entity MUST be persisted in database");

        User user = optionalUser.get();
        assertEquals("Sunita Sharma", user.getFullName());
        assertEquals(email, user.getEmail());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertTrue(user.getRoles().contains(UserRole.ROLE_DONOR));

        // Security assertion: Plaintext password is NEVER stored
        assertNotEquals(rawPassword, user.getPasswordHash(), "Plaintext password must never be stored");
        assertTrue(user.getPasswordHash().startsWith("$2a$") || user.getPasswordHash().startsWith("$2b$") || user.getPasswordHash().startsWith("$2y$"),
                "Password must be securely hashed with BCrypt");
        assertTrue(passwordEncoder.matches(rawPassword, user.getPasswordHash()),
                "BCrypt password encoder must verify raw password against stored hash");
    }

    @Test
    @DisplayName("Persistence 2: Logout must NOT delete the user account from the database")
    void testLogoutDoesNotDeleteUserAccount() throws Exception {
        String email = "logout.test." + UUID.randomUUID() + "@netra.org";
        String rawPassword = "SecurePass123";
        RegisterRequest request = new RegisterRequest("Amit Kumar", email, uniquePhone(), rawPassword);

        MvcResult regResult = mockMvc.perform(post("/api/v1/auth/register")
                .with(req -> { req.setRemoteAddr("127.1.2.2"); return req; })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(regResult.getResponse().getContentAsString());
        String accessToken = json.get("accessToken").asText();
        String refreshToken = json.get("refreshToken").asText();

        // Perform logout
        mockMvc.perform(post("/api/v1/auth/logout")
                .with(req -> { req.setRemoteAddr("127.1.2.2"); return req; })
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LogoutRequest(refreshToken))))
                .andExpect(status().isOk());

        // CRITICAL INVARIANT: The user row MUST NOT be deleted
        Optional<User> userAfterLogout = userRepository.findByEmailIgnoreCase(email);
        assertTrue(userAfterLogout.isPresent(), "User account must remain in database after logout");
        assertEquals(UserStatus.ACTIVE, userAfterLogout.get().getStatus(), "User account status must remain ACTIVE");
        assertTrue(passwordEncoder.matches(rawPassword, userAfterLogout.get().getPasswordHash()),
                "Password hash must remain intact and valid after logout");

        // User must be able to log in again with original credentials
        mockMvc.perform(post("/api/v1/auth/login")
                .with(req -> { req.setRemoteAddr("127.1.2.2"); return req; })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(email, rawPassword))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", notNullValue()))
                .andExpect(jsonPath("$.user.email", is(email)));
    }

    @Test
    @DisplayName("Persistence 3: Logout-all invalidates tokens but preserves user account and credentials")
    void testLogoutAllPreservesUserAccount() throws Exception {
        String email = "logoutall." + UUID.randomUUID() + "@netra.org";
        String rawPassword = "GlobalLogoutPass123";
        RegisterRequest request = new RegisterRequest("Priya Roy", email, uniquePhone(), rawPassword);

        MvcResult regResult = mockMvc.perform(post("/api/v1/auth/register")
                .with(req -> { req.setRemoteAddr("127.1.2.3"); return req; })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(regResult.getResponse().getContentAsString());
        String accessToken = json.get("accessToken").asText();

        // Perform logout-all
        mockMvc.perform(post("/api/v1/auth/logout-all")
                .with(req -> { req.setRemoteAddr("127.1.2.3"); return req; })
                .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        // CRITICAL INVARIANT: User account remains intact
        Optional<User> userAfterLogoutAll = userRepository.findByEmailIgnoreCase(email);
        assertTrue(userAfterLogoutAll.isPresent(), "User account must remain in database after logout-all");
        assertEquals(UserStatus.ACTIVE, userAfterLogoutAll.get().getStatus());

        // Re-login succeeds
        mockMvc.perform(post("/api/v1/auth/login")
                .with(req -> { req.setRemoteAddr("127.1.2.3"); return req; })
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(email, rawPassword))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email", is(email)));
    }

    @Test
    @DisplayName("Persistence 4: File-backed database survives process restart simulation without data loss")
    void testFileBackedDatabasePersistenceAcrossRestarts() throws Exception {
        File tempDir = new File("target/test-db-persistence");
        tempDir.mkdirs();
        String dbPath = tempDir.getAbsolutePath() + "/netra_test_persist";
        String jdbcUrl = "jdbc:h2:file:" + dbPath + ";AUTO_SERVER=TRUE;MODE=PostgreSQL;DEFAULT_LOCK_TIMEOUT=30000";

        String testEmail = "restart.test." + UUID.randomUUID() + "@netra.org";
        String rawPassword = "PersistAcrossRestarts1";
        String hashedPassword = passwordEncoder.encode(rawPassword);

        // 1. First "Run" (Process 1): Create schema, insert user, and close connection completely
        try (Connection conn = DriverManager.getConnection(jdbcUrl, "sa", "")) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE IF NOT EXISTS test_users (" +
                        "id VARCHAR(36) PRIMARY KEY, " +
                        "email VARCHAR(255) NOT NULL, " +
                        "password_hash VARCHAR(255) NOT NULL, " +
                        "status VARCHAR(32) NOT NULL)");
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO test_users (id, email, password_hash, status) VALUES (?, ?, ?, ?)")) {
                ps.setString(1, UUID.randomUUID().toString());
                ps.setString(2, testEmail);
                ps.setString(3, hashedPassword);
                ps.setString(4, "ACTIVE");
                ps.executeUpdate();
            }
        }
        // At this point, Connection 1 is completely closed (simulating Spring Boot shutdown)

        // 2. Second "Run" (Process 2 / Restart): Reopen connection to the same file URL
        try (Connection conn2 = DriverManager.getConnection(jdbcUrl, "sa", "")) {
            try (PreparedStatement ps2 = conn2.prepareStatement(
                    "SELECT email, password_hash, status FROM test_users WHERE email = ?")) {
                ps2.setString(1, testEmail);
                try (ResultSet rs = ps2.executeQuery()) {
                    assertTrue(rs.next(), "User record MUST persist in file-backed database across restart");
                    assertEquals(testEmail, rs.getString("email"));
                    assertEquals("ACTIVE", rs.getString("status"));
                    String reloadedHash = rs.getString("password_hash");
                    assertTrue(passwordEncoder.matches(rawPassword, reloadedHash),
                            "BCrypt password verification must succeed for reloaded user row");
                }
            }
        }
    }
}
