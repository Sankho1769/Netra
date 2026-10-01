package org.netra.features.auth;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.netra.NetraApplication;
import org.netra.features.auth.dto.AuthResponse;
import org.netra.features.auth.dto.LoginRequest;
import org.netra.features.auth.dto.LogoutRequest;
import org.netra.features.auth.dto.RegisterRequest;
import org.netra.features.auth.service.AuthService;
import org.netra.features.user.entity.User;
import org.netra.features.user.entity.UserRole;
import org.netra.features.user.entity.UserStatus;
import org.netra.features.user.repository.UserRepository;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.io.File;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PHASE 1F: Real File-Backed Application Context Restart Persistence Test.
 *
 * Proves conclusively that:
 * 1. User registration commits to a real file-backed database on disk.
 * 2. Stopping Spring Boot completely and starting a new Spring Application Context
 *    retains the exact same user ID, email, and BCrypt password hash.
 * 3. The persistent password hash remains verifiable by the authentication manager.
 * 4. Login succeeds after complete application context restart.
 * 5. Logout does NOT delete the user account from the persistent database.
 */
class ApplicationDatabasePersistenceIntegrationTest {

    private static final String TEST_DB_PATH = "./target/persistence-test/netradb";
    private static final String TEST_JDBC_URL = "jdbc:h2:file:" + TEST_DB_PATH + ";AUTO_SERVER=TRUE;MODE=PostgreSQL;DEFAULT_LOCK_TIMEOUT=30000";

    @BeforeAll
    static void setupDbDirectory() {
        File dir = new File("./target/persistence-test");
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    f.delete();
                }
            }
        } else {
            dir.mkdirs();
        }
    }

    @AfterAll
    static void cleanupDbDirectory() {
        File dir = new File("./target/persistence-test");
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    f.delete();
                }
            }
        }
    }

    @Test
    @DisplayName("Verify User persists across complete Spring Boot application context shutdown and restart")
    void verifyUserPersistenceAcrossContextRestart() {
        final String testEmail = "persistent.donor." + System.currentTimeMillis() + "@netra.org";
        final String testPassword = "SecurePassword123";
        final String testPhone = "+919876543210";
        final String testName = "Dr. Persistent Donor";

        UUID registeredUserId;
        String registeredPasswordHash;

        // =====================================================================
        // STEP 1: Start Application Context 1 (First lifecycle instance)
        // =====================================================================
        ConfigurableApplicationContext context1 = new SpringApplicationBuilder(NetraApplication.class)
                .run(
                        "--server.port=0",
                        "--spring.datasource.url=" + TEST_JDBC_URL,
                        "--spring.datasource.driver-class-name=org.h2.Driver",
                        "--spring.datasource.username=sa",
                        "--spring.datasource.password=",
                        "--spring.jpa.hibernate.ddl-auto=update",
                        "--spring.flyway.enabled=false",
                        "--netra.notifications.push.provider=noop"
                );

        try {
            AuthService authService1 = context1.getBean(AuthService.class);
            UserRepository userRepository1 = context1.getBean(UserRepository.class);

            // Register new test account
            RegisterRequest registerRequest = new RegisterRequest(testName, testEmail, testPhone, testPassword);
            AuthResponse regResponse = authService1.register(registerRequest, "127.0.0.1", "JUnit5-TestAgent-1");

            assertThat(regResponse).isNotNull();
            assertThat(regResponse.getAccessToken()).isNotEmpty();
            assertThat(regResponse.getUser().getEmail()).isEqualTo(testEmail);

            registeredUserId = regResponse.getUser().getId();

            // Direct repository query in Context 1
            User savedUser = userRepository1.findById(registeredUserId)
                    .orElseThrow(() -> new AssertionError("User was not found in repository after registration"));

            assertThat(savedUser.getId()).isEqualTo(registeredUserId);
            assertThat(savedUser.getEmail()).isEqualTo(testEmail);
            assertThat(savedUser.getFullName()).isEqualTo(testName);
            assertThat(savedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(savedUser.getRoles()).contains(UserRole.ROLE_DONOR);

            registeredPasswordHash = savedUser.getPasswordHash();
            assertThat(registeredPasswordHash).isNotEmpty();
            assertThat(registeredPasswordHash).startsWith("$2"); // BCrypt signature

        } finally {
            // STEP 2: Completely stop and terminate Spring Boot Context 1
            context1.close();
        }

        // Verify the database file physically exists on the filesystem
        File dbFile = new File("./target/persistence-test/netradb.mv.db");
        assertThat(dbFile.exists()).isTrue();
        assertThat(dbFile.length()).isGreaterThan(0);

        // =====================================================================
        // STEP 3: Start Application Context 2 (Completely fresh instance, same DB file)
        // =====================================================================
        ConfigurableApplicationContext context2 = new SpringApplicationBuilder(NetraApplication.class)
                .run(
                        "--server.port=0",
                        "--spring.datasource.url=" + TEST_JDBC_URL,
                        "--spring.datasource.driver-class-name=org.h2.Driver",
                        "--spring.datasource.username=sa",
                        "--spring.datasource.password=",
                        "--spring.jpa.hibernate.ddl-auto=update",
                        "--spring.flyway.enabled=false",
                        "--netra.notifications.push.provider=noop"
                );


        try {
            AuthService authService2 = context2.getBean(AuthService.class);
            UserRepository userRepository2 = context2.getBean(UserRepository.class);
            PasswordEncoder passwordEncoder2 = context2.getBean(PasswordEncoder.class);

            // Directly query Context 2's repository to prove the user record remained intact on disk
            User reloadedUser = userRepository2.findByEmailIgnoreCase(testEmail)
                    .orElseThrow(() -> new AssertionError("User account disappeared after Spring Boot context restart!"));

            assertThat(reloadedUser.getId()).isEqualTo(registeredUserId);
            assertThat(reloadedUser.getEmail()).isEqualTo(testEmail);
            assertThat(reloadedUser.getFullName()).isEqualTo(testName);
            assertThat(reloadedUser.getPhone()).isEqualTo(testPhone);
            assertThat(reloadedUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
            assertThat(reloadedUser.getPasswordHash()).isEqualTo(registeredPasswordHash);

            // Password hash must still correctly verify the original password
            assertThat(passwordEncoder2.matches(testPassword, reloadedUser.getPasswordHash())).isTrue();

            // STEP 4: Authenticate with the same credentials in Context 2
            LoginRequest loginRequest = new LoginRequest(testEmail, testPassword, "device-persisted-1");
            AuthResponse loginResponse = authService2.login(loginRequest, "127.0.0.1", "JUnit5-TestAgent-2");

            assertThat(loginResponse).isNotNull();
            assertThat(loginResponse.getAccessToken()).isNotEmpty();
            assertThat(loginResponse.getRefreshToken()).isNotEmpty();
            assertThat(loginResponse.getUser().getId()).isEqualTo(registeredUserId);
            assertThat(loginResponse.getUser().getEmail()).isEqualTo(testEmail);

            // STEP 5: Logout must NOT delete the user account
            LogoutRequest logoutRequest = new LogoutRequest(loginResponse.getRefreshToken());
            authService2.logout(logoutRequest, registeredUserId, "127.0.0.1", "JUnit5-TestAgent-2");

            // User must STILL exist in the repository after logout
            User postLogoutUser = userRepository2.findById(registeredUserId)
                    .orElseThrow(() -> new AssertionError("User was unexpectedly deleted upon logout!"));

            assertThat(postLogoutUser.getId()).isEqualTo(registeredUserId);
            assertThat(postLogoutUser.getStatus()).isEqualTo(UserStatus.ACTIVE);

            // Can log in again after logout
            AuthResponse reLoginResponse = authService2.login(loginRequest, "127.0.0.1", "JUnit5-TestAgent-2");
            assertThat(reLoginResponse).isNotNull();
            assertThat(reLoginResponse.getUser().getId()).isEqualTo(registeredUserId);

        } finally {
            // Terminate Context 2
            context2.close();
        }
    }
}
