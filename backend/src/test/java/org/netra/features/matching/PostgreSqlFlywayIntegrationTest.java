package org.netra.features.matching;

import org.junit.jupiter.api.*;
import org.netra.features.matching.entity.MatchStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Real PostgreSQL and Flyway integration test infrastructure using Testcontainers.
 *
 * Verifies:
 * 1. Clean Flyway execution of migrations V1 through V12 on a clean PostgreSQL 16 container.
 * 2. Complete schema metadata verification (tables, columns, PK, FK, unique, check, indexes, history).
 * 3. Functional behavior of donor_matches (insert, duplicate reject, status check reject, FK reject, UUID gen).
 * 4. Database-level concurrency invariants on real PostgreSQL (duplicate protection, atomic CAS transitions).
 *
 * Environmental Resilience:
 * - When Docker is available, automatically provisions a clean postgres:16-alpine container, enables Flyway,
 *   sets Hibernate ddl-auto=validate, and executes all real PostgreSQL assertions.
 * - When Docker is unavailable, safely skips execution via JUnit 5 assumption, explicitly documenting
 *   the environmental blocker ("Docker is not available in current host environment").
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PostgreSqlFlywayIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(PostgreSqlFlywayIntegrationTest.class);
    private static final boolean DOCKER_AVAILABLE;
    private static PostgreSQLContainer<?> postgres = null;

    static {
        if (System.getProperty("api.version") == null) {
            System.setProperty("api.version", "1.44");
        }
        boolean available = false;
        try {
            available = DockerClientFactory.instance().isDockerAvailable();
            if (available) {
                postgres = new PostgreSQLContainer<>("postgres:16-alpine")
                        .withDatabaseName("netra_test")
                        .withUsername("netra_user")
                        .withPassword("netra_pass");
                postgres.start();
                log.info("Testcontainers PostgreSQL 16 container started at {}", postgres.getJdbcUrl());
            } else {
                log.warn("Docker environment is not available. Testcontainers execution will be skipped.");
            }
        } catch (Throwable t) {
            log.warn("Testcontainers initialization encountered environment limitation: {}", t.getMessage());
            available = false;
        }
        DOCKER_AVAILABLE = available;
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        if (DOCKER_AVAILABLE && postgres != null && postgres.isRunning()) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl);
            registry.add("spring.datasource.username", postgres::getUsername);
            registry.add("spring.datasource.password", postgres::getPassword);
            registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
            registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
            registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
            registry.add("spring.flyway.enabled", () -> "true");
        }
    }

    @AfterAll
    static void tearDownContainer() {
        if (postgres != null && postgres.isRunning()) {
            postgres.stop();
        }
    }

    @Autowired(required = false)
    private DataSource dataSource;

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void verifyEnvironment() {
        assumeTrue(DOCKER_AVAILABLE && postgres != null && postgres.isRunning(),
                "Docker is not available in the current environment. Real PostgreSQL integration test requires an active Docker daemon.");
    }

    @Test
    @Order(1)
    @DisplayName("PostgreSQL Gate: Verify Flyway V1->V14 executed cleanly and created all schema objects")
    void testFlywayV1ThroughV14SchemaMetadata() throws Exception {
        assertNotNull(dataSource, "DataSource must be injected");
        assertNotNull(jdbcTemplate, "JdbcTemplate must be injected");

        // 1. Verify Flyway schema history table exists and contains 14 successful migrations
        List<Map<String, Object>> history = jdbcTemplate.queryForList(
                "SELECT version, description, type, script, success FROM flyway_schema_history ORDER BY installed_rank"
        );
        assertEquals(14, history.size(), "Flyway must have applied exactly 14 migrations (V1 through V14)");

        for (Map<String, Object> row : history) {
            Boolean success = (Boolean) row.get("success");
            assertTrue(Boolean.TRUE.equals(success), "Migration " + row.get("version") + " must be marked as successful");
        }

        // 2. Verify all expected platform tables exist in PostgreSQL
        Set<String> expectedTables = Set.of(
                "eligibility_questions", "eligibility_rules", "deferral_reasons",
                "eligibility_sessions", "eligibility_answers", "eligibility_audit_logs",
                "users", "user_roles", "refresh_sessions", "security_audit_logs",
                "donor_profiles", "blood_banks", "blood_inventory", "blood_bank_accounts",
                "donation_events", "donation_event_registrations", "blood_requests",
                "idempotency_records", "donor_matches", "notifications", "user_device_tokens",
                "donations"
        );

        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            Set<String> actualTables = new HashSet<>();
            try (ResultSet rs = meta.getTables(null, "public", "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    actualTables.add(rs.getString("TABLE_NAME").toLowerCase());
                }
            }

            for (String expected : expectedTables) {
                assertTrue(actualTables.contains(expected), "Expected table '" + expected + "' must exist in PostgreSQL");
            }

            // 3. Verify donor_matches column structure
            Set<String> matchColumns = new HashSet<>();
            try (ResultSet rs = meta.getColumns(null, "public", "donor_matches", "%")) {
                while (rs.next()) {
                    matchColumns.add(rs.getString("COLUMN_NAME").toLowerCase());
                }
            }

            Set<String> expectedColumns = Set.of(
                    "id", "blood_request_id", "donor_user_id", "response_status",
                    "created_at", "updated_at", "responded_at", "expires_at", "version"
            );
            assertTrue(matchColumns.containsAll(expectedColumns), "donor_matches must contain all 9 expected columns");

            // 4. Verify unique constraint uq_donor_matches_request_donor
            Integer uqCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM pg_constraint WHERE conname = 'uq_donor_matches_request_donor'",
                    Integer.class
            );
            assertNotNull(uqCount);
            assertEquals(1, uqCount, "Unique constraint 'uq_donor_matches_request_donor' must exist in PostgreSQL");

            // 5. Verify check constraint chk_donor_matches_status
            Integer chkCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM pg_constraint WHERE conname = 'chk_donor_matches_status'",
                    Integer.class
            );
            assertNotNull(chkCount);
            assertEquals(1, chkCount, "Check constraint 'chk_donor_matches_status' must exist in PostgreSQL");

            // 6. Verify foreign keys
            Integer fkReqCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM pg_constraint WHERE conname = 'fk_donor_matches_request'",
                    Integer.class
            );
            assertEquals(1, fkReqCount, "Foreign key 'fk_donor_matches_request' must exist in PostgreSQL");

            Integer fkDonorCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM pg_constraint WHERE conname = 'fk_donor_matches_donor'",
                    Integer.class
            );
            assertEquals(1, fkDonorCount, "Foreign key 'fk_donor_matches_donor' must exist in PostgreSQL");

            // 7. Verify indexes
            List<String> indexes = jdbcTemplate.queryForList(
                    "SELECT indexname FROM pg_indexes WHERE tablename = 'donor_matches'",
                    String.class
            );
            assertTrue(indexes.contains("idx_donor_matches_blood_request_id"), "Index idx_donor_matches_blood_request_id must exist");
            assertTrue(indexes.contains("idx_donor_matches_donor_user_id"), "Index idx_donor_matches_donor_user_id must exist");
            assertTrue(indexes.contains("idx_donor_matches_status_expires"), "Index idx_donor_matches_status_expires must exist");

            // 8. Verify notifications column structure
            Set<String> notificationColumns = new HashSet<>();
            try (ResultSet rs = meta.getColumns(null, "public", "notifications", "%")) {
                while (rs.next()) {
                    notificationColumns.add(rs.getString("COLUMN_NAME").toLowerCase());
                }
            }
            Set<String> expectedNotificationColumns = Set.of(
                    "id", "recipient_user_id", "type", "title", "body", "reference_type",
                    "reference_id", "created_at", "updated_at", "read_at", "delivery_status",
                    "idempotency_key", "version"
            );
            assertTrue(notificationColumns.containsAll(expectedNotificationColumns), "notifications must contain all 13 expected columns");

            // 9. Verify user_device_tokens column structure
            Set<String> tokenColumns = new HashSet<>();
            try (ResultSet rs = meta.getColumns(null, "public", "user_device_tokens", "%")) {
                while (rs.next()) {
                    tokenColumns.add(rs.getString("COLUMN_NAME").toLowerCase());
                }
            }
            Set<String> expectedTokenColumns = Set.of(
                    "id", "user_id", "token", "token_hash", "provider", "platform",
                    "active", "created_at", "updated_at", "last_seen_at", "revoked_at"
            );
            assertTrue(tokenColumns.containsAll(expectedTokenColumns), "user_device_tokens must contain all 11 expected columns");

            // 10. Verify V13 constraints
            Integer uqIdempCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM pg_constraint WHERE conname = 'uq_notifications_idempotency_key'",
                    Integer.class
            );
            assertEquals(1, uqIdempCount, "Unique constraint 'uq_notifications_idempotency_key' must exist in PostgreSQL");

            Integer uqTokenCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM pg_constraint WHERE conname = 'uq_user_device_tokens_token'",
                    Integer.class
            );
            assertEquals(1, uqTokenCount, "Unique constraint 'uq_user_device_tokens_token' must exist in PostgreSQL");

            Integer chkProviderCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM pg_constraint WHERE conname = 'chk_device_tokens_provider'",
                    Integer.class
            );
            assertEquals(1, chkProviderCount, "Check constraint 'chk_device_tokens_provider' must exist in PostgreSQL");

            // 11. Verify V13 indexes
            List<String> notifIndexes = jdbcTemplate.queryForList(
                    "SELECT indexname FROM pg_indexes WHERE tablename = 'notifications'",
                    String.class
            );
            assertTrue(notifIndexes.contains("idx_notifications_recipient_created"), "Index idx_notifications_recipient_created must exist");
            assertTrue(notifIndexes.contains("idx_notifications_recipient_unread"), "Index idx_notifications_recipient_unread must exist");
            assertTrue(notifIndexes.contains("idx_notifications_reference"), "Index idx_notifications_reference must exist");

            List<String> tokenIndexes = jdbcTemplate.queryForList(
                    "SELECT indexname FROM pg_indexes WHERE tablename = 'user_device_tokens'",
                    String.class
            );
            assertTrue(tokenIndexes.contains("idx_device_tokens_user_active"), "Index idx_device_tokens_user_active must exist");
            assertTrue(tokenIndexes.contains("idx_device_tokens_token_hash"), "Index idx_device_tokens_token_hash must exist");

            // 12. Verify V14 donations constraints
            Integer chkDonationSourceType = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM pg_constraint WHERE conname = 'chk_donations_source_type'",
                    Integer.class
            );
            assertEquals(1, chkDonationSourceType, "Check constraint 'chk_donations_source_type' must exist");

            Integer chkDonationStatus = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM pg_constraint WHERE conname = 'chk_donations_verification_status'",
                    Integer.class
            );
            assertEquals(1, chkDonationStatus, "Check constraint 'chk_donations_verification_status' must exist");

            Integer chkDonationSourceRef = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM pg_constraint WHERE conname = 'chk_donations_source_references'",
                    Integer.class
            );
            assertEquals(1, chkDonationSourceRef, "Check constraint 'chk_donations_source_references' must exist");

            Integer uqDonationRequest = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM pg_constraint WHERE conname = 'uq_donations_donor_blood_request'",
                    Integer.class
            );
            assertEquals(1, uqDonationRequest, "Unique constraint 'uq_donations_donor_blood_request' must exist");

            Integer uqDonationEvent = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM pg_constraint WHERE conname = 'uq_donations_donor_event'",
                    Integer.class
            );
            assertEquals(1, uqDonationEvent, "Unique constraint 'uq_donations_donor_event' must exist");

            // 13. Verify V14 donations indexes
            List<String> donationIndexes = jdbcTemplate.queryForList(
                    "SELECT indexname FROM pg_indexes WHERE tablename = 'donations'",
                    String.class
            );
            assertTrue(donationIndexes.contains("idx_donations_donor_status_date"), "Index idx_donations_donor_status_date must exist");
            assertTrue(donationIndexes.contains("idx_donations_status_created"), "Index idx_donations_status_created must exist");
            assertTrue(donationIndexes.contains("idx_donations_blood_request"), "Index idx_donations_blood_request must exist");
            assertTrue(donationIndexes.contains("idx_donations_donation_event"), "Index idx_donations_donation_event must exist");
        }
    }

    @Test
    @Order(2)
    @DisplayName("PostgreSQL Functional: Verify donor_matches constraints, insertion, and UUID generation")
    void testV12BehaviorOnPostgreSQL() {
        // Seed prerequisites: 1 requester, 1 donor, 1 blood request
        UUID requesterId = UUID.randomUUID();
        UUID donorId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();

        jdbcTemplate.update(
                "INSERT INTO users (id, full_name, email, password_hash, status) VALUES (?, 'Requester User', ?, 'hash', 'ACTIVE')",
                requesterId, "req_" + requesterId + "@netra.org"
        );
        jdbcTemplate.update(
                "INSERT INTO users (id, full_name, email, password_hash, status) VALUES (?, 'Donor User', ?, 'hash', 'ACTIVE')",
                donorId, "donor_" + donorId + "@netra.org"
        );
        jdbcTemplate.update(
                "INSERT INTO blood_requests (id, requester_user_id, blood_group, units_required, urgency, status, " +
                        "hospital_name, hospital_address, city, state, postal_code, latitude, longitude, required_by) " +
                        "VALUES (?, ?, 'O+', 1, 'NORMAL', 'OPEN', 'City Hospital', 'Main St', 'Mumbai', 'MH', '400001', 19.07, 72.87, ?)",
                requestId, requesterId, Timestamp.from(Instant.now().plus(24, ChronoUnit.HOURS))
        );

        // A. Insert valid donor match using default UUID generation
        Instant expiresAt = Instant.now().plus(24, ChronoUnit.HOURS);
        Timestamp expiresAtTs = Timestamp.from(expiresAt);
        jdbcTemplate.update(
                "INSERT INTO donor_matches (blood_request_id, donor_user_id, response_status, expires_at) " +
                        "VALUES (?, ?, 'MATCHED', ?)",
                requestId, donorId, expiresAtTs
        );

        Map<String, Object> inserted = jdbcTemplate.queryForMap(
                "SELECT id, response_status, version, created_at, expires_at FROM donor_matches WHERE blood_request_id = ? AND donor_user_id = ?",
                requestId, donorId
        );
        assertNotNull(inserted.get("id"), "UUID primary key must be auto-generated via gen_random_uuid()");
        assertEquals("MATCHED", inserted.get("response_status"));
        assertEquals(0L, ((Number) inserted.get("version")).longValue());
        assertNotNull(inserted.get("created_at"));
        assertNotNull(inserted.get("expires_at"));

        // B. Duplicate (blood_request_id, donor_user_id) rejected by UNIQUE constraint
        assertThrows(DataIntegrityViolationException.class, () -> {
            jdbcTemplate.update(
                    "INSERT INTO donor_matches (blood_request_id, donor_user_id, response_status, expires_at) " +
                            "VALUES (?, ?, 'MATCHED', ?)",
                    requestId, donorId, expiresAtTs
            );
        }, "Inserting duplicate (request, donor) must violate uq_donor_matches_request_donor");

        // C. Invalid response_status rejected by CHECK constraint
        UUID anotherDonorId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO users (id, full_name, email, password_hash, status) VALUES (?, 'Donor 2', ?, 'hash', 'ACTIVE')",
                anotherDonorId, "donor2_" + anotherDonorId + "@netra.org"
        );

        assertThrows(DataIntegrityViolationException.class, () -> {
            jdbcTemplate.update(
                    "INSERT INTO donor_matches (blood_request_id, donor_user_id, response_status, expires_at) " +
                            "VALUES (?, ?, 'INVALID_STATUS', ?)",
                    requestId, anotherDonorId, expiresAtTs
            );
        }, "Invalid response_status must violate chk_donor_matches_status");

        // D. Nonexistent blood_request_id rejected by FK
        assertThrows(DataIntegrityViolationException.class, () -> {
            jdbcTemplate.update(
                    "INSERT INTO donor_matches (blood_request_id, donor_user_id, response_status, expires_at) " +
                            "VALUES (?, ?, 'MATCHED', ?)",
                    UUID.randomUUID(), anotherDonorId, expiresAtTs
            );
        }, "Nonexistent blood_request_id must violate fk_donor_matches_request");

        // E. Nonexistent donor_user_id rejected by FK
        assertThrows(DataIntegrityViolationException.class, () -> {
            jdbcTemplate.update(
                    "INSERT INTO donor_matches (blood_request_id, donor_user_id, response_status, expires_at) " +
                            "VALUES (?, ?, 'MATCHED', ?)",
                    requestId, UUID.randomUUID(), expiresAtTs
            );
        }, "Nonexistent donor_user_id must violate fk_donor_matches_donor");
    }

    @Test
    @Order(3)
    @DisplayName("PostgreSQL Concurrency: Verify atomic transitions and duplicate race isolation on PostgreSQL")
    void testPostgreSqlConcurrencyInvariants() throws Exception {
        UUID requesterId = UUID.randomUUID();
        UUID donorId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();

        jdbcTemplate.update(
                "INSERT INTO users (id, full_name, email, password_hash, status) VALUES (?, 'Req C', ?, 'hash', 'ACTIVE')",
                requesterId, "req_c_" + requesterId + "@netra.org"
        );
        jdbcTemplate.update(
                "INSERT INTO users (id, full_name, email, password_hash, status) VALUES (?, 'Donor C', ?, 'hash', 'ACTIVE')",
                donorId, "donor_c_" + donorId + "@netra.org"
        );
        jdbcTemplate.update(
                "INSERT INTO blood_requests (id, requester_user_id, blood_group, units_required, urgency, status, " +
                        "hospital_name, hospital_address, city, state, postal_code, latitude, longitude, required_by) " +
                        "VALUES (?, ?, 'A+', 1, 'NORMAL', 'OPEN', 'Hosp C', 'St C', 'Pune', 'MH', '411001', 18.52, 73.85, ?)",
                requestId, requesterId, Timestamp.from(Instant.now().plus(24, ChronoUnit.HOURS))
        );

        // 1. Concurrent duplicate creation race: only 1 insert succeeds
        int threads = 4;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();
        Instant expiresAt = Instant.now().plus(24, ChronoUnit.HOURS);
        Timestamp expiresAtTs = Timestamp.from(expiresAt);

        for (int i = 0; i < threads; i++) {
            futures.add(executor.submit(() -> {
                try {
                    startLatch.await();
                    jdbcTemplate.update(
                            "INSERT INTO donor_matches (blood_request_id, donor_user_id, response_status, expires_at) " +
                                    "VALUES (?, ?, 'MATCHED', ?)",
                            requestId, donorId, expiresAtTs
                    );
                    successCount.incrementAndGet();
                } catch (DataIntegrityViolationException ex) {
                    conflictCount.incrementAndGet();
                } catch (Exception e) {
                    // unexpected error
                }
            }));
        }

        startLatch.countDown();
        for (Future<?> f : futures) {
            f.get(5, TimeUnit.SECONDS);
        }

        assertEquals(1, successCount.get(), "Exactly one concurrent match creation must succeed");
        assertEquals(threads - 1, conflictCount.get(), "Other concurrent match creations must encounter unique conflict");

        // 2. Concurrent accept attempts: atomic CAS update ensures exactly 1 success
        UUID matchId = jdbcTemplate.queryForObject(
                "SELECT id FROM donor_matches WHERE blood_request_id = ? AND donor_user_id = ?",
                UUID.class, requestId, donorId
        );
        assertNotNull(matchId);

        CountDownLatch acceptLatch = new CountDownLatch(1);
        AtomicInteger acceptSuccess = new AtomicInteger(0);
        AtomicInteger acceptFailed = new AtomicInteger(0);
        List<Future<?>> acceptFutures = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            acceptFutures.add(executor.submit(() -> {
                try {
                    acceptLatch.await();
                    int updated = jdbcTemplate.update(
                            "UPDATE donor_matches SET response_status = 'ACCEPTED', responded_at = now(), updated_at = now(), version = version + 1 " +
                                    "WHERE id = ? AND donor_user_id = ? AND response_status = 'MATCHED' AND expires_at > now()",
                            matchId, donorId
                    );
                    if (updated == 1) {
                        acceptSuccess.incrementAndGet();
                    } else {
                        acceptFailed.incrementAndGet();
                    }
                } catch (Exception e) {
                    // unexpected
                }
            }));
        }

        acceptLatch.countDown();
        for (Future<?> f : acceptFutures) {
            f.get(5, TimeUnit.SECONDS);
        }

        executor.shutdown();

        assertEquals(1, acceptSuccess.get(), "Exactly one concurrent accept transition must succeed");
        assertEquals(threads - 1, acceptFailed.get(), "Competing accept attempts must find 0 rows matching status = 'MATCHED'");

        String finalStatus = jdbcTemplate.queryForObject(
                "SELECT response_status FROM donor_matches WHERE id = ?",
                String.class, matchId
        );
        assertEquals("ACCEPTED", finalStatus, "Final status must be immutable terminal ACCEPTED");
    }

    @Test
    @Order(4)
    @DisplayName("PostgreSQL Functional: Verify notifications and user_device_tokens constraints and UUID generation")
    void testV13NotificationsBehaviorOnPostgreSQL() {
        UUID userId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO users (id, full_name, email, password_hash, status) VALUES (?, 'Notif User', ?, 'hash', 'ACTIVE')",
                userId, "notif_" + userId + "@netra.org"
        );

        // 1. Insert notification with default UUID generation
        String idempotencyKey = "NOTIF_TEST:" + UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO notifications (recipient_user_id, type, title, body, delivery_status, idempotency_key) " +
                        "VALUES (?, 'MATCH_CREATED', 'Test Notification', 'You have a match', 'PENDING', ?)",
                userId, idempotencyKey
        );

        Map<String, Object> inserted = jdbcTemplate.queryForMap(
                "SELECT id, recipient_user_id, type, delivery_status, version, created_at, read_at FROM notifications WHERE idempotency_key = ?",
                idempotencyKey
        );
        assertNotNull(inserted.get("id"), "PostgreSQL gen_random_uuid() must auto-populate id");
        assertEquals(userId, inserted.get("recipient_user_id"));
        assertEquals("MATCH_CREATED", inserted.get("type"));
        assertEquals("PENDING", inserted.get("delivery_status"));
        assertEquals(0L, ((Number) inserted.get("version")).longValue());
        assertNull(inserted.get("read_at"));

        // 2. Duplicate idempotency key must violate unique constraint uq_notifications_idempotency_key
        assertThrows(DataIntegrityViolationException.class, () ->
                jdbcTemplate.update(
                        "INSERT INTO notifications (recipient_user_id, type, title, body, delivery_status, idempotency_key) " +
                                "VALUES (?, 'MATCH_CREATED', 'Duplicate', 'Body', 'PENDING', ?)",
                        userId, idempotencyKey
                )
        );

        // 3. Invalid delivery status must violate chk_notifications_delivery_status
        assertThrows(DataIntegrityViolationException.class, () ->
                jdbcTemplate.update(
                        "INSERT INTO notifications (recipient_user_id, type, title, body, delivery_status) " +
                                "VALUES (?, 'MATCH_CREATED', 'Invalid Status', 'Body', 'INVALID_STATUS')",
                        userId
                )
        );

        // 3b. NO_DEVICES delivery status is valid and accepted
        jdbcTemplate.update(
                "INSERT INTO notifications (recipient_user_id, type, title, body, delivery_status) " +
                        "VALUES (?, 'MATCH_CREATED', 'No Devices Status', 'Body', 'NO_DEVICES')",
                userId
        );

        // 4. Insert user device token with default UUID generation
        String deviceToken = "fcm_token_" + UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO user_device_tokens (user_id, token, token_hash, platform, provider) " +
                        "VALUES (?, ?, 'hash123', 'ANDROID', 'FCM')",
                userId, deviceToken
        );

        Map<String, Object> insertedToken = jdbcTemplate.queryForMap(
                "SELECT id, user_id, active, platform, provider, created_at FROM user_device_tokens WHERE token = ?",
                deviceToken
        );
        assertNotNull(insertedToken.get("id"), "PostgreSQL gen_random_uuid() must auto-populate token id");
        assertEquals(true, insertedToken.get("active"));
        assertEquals("ANDROID", insertedToken.get("platform"));

        // 5. Duplicate token must violate uq_user_device_tokens_token
        assertThrows(DataIntegrityViolationException.class, () ->
                jdbcTemplate.update(
                        "INSERT INTO user_device_tokens (user_id, token, token_hash, platform, provider) " +
                                "VALUES (?, ?, 'hash456', 'ANDROID', 'FCM')",
                        userId, deviceToken
                )
        );

        // 6. Invalid platform must violate chk_device_tokens_platform
        assertThrows(DataIntegrityViolationException.class, () ->
                jdbcTemplate.update(
                        "INSERT INTO user_device_tokens (user_id, token, token_hash, platform, provider) " +
                                "VALUES (?, 'token_invalid_plat', 'hash789', 'WINDOWS_PHONE', 'FCM')",
                        userId
                )
        );

        // 7. Invalid provider must violate chk_device_tokens_provider
        assertThrows(DataIntegrityViolationException.class, () ->
                jdbcTemplate.update(
                        "INSERT INTO user_device_tokens (user_id, token, token_hash, platform, provider) " +
                                "VALUES (?, 'token_invalid_prov', 'hashprov', 'ANDROID', 'APNS')",
                        userId
                )
        );
    }

    @Test
    @Order(5)
    @DisplayName("PostgreSQL Functional: Verify donations V14 constraints, source references, uniqueness, and UUID generation")
    void testV14BehaviorOnPostgreSQL() {
        // Seed prerequisites
        UUID donorId = UUID.randomUUID();
        UUID bankId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        UUID eventId = UUID.randomUUID();
        UUID verifierId = UUID.randomUUID();

        // 1. Insert users
        jdbcTemplate.update(
                "INSERT INTO users (id, full_name, email, password_hash, status) " +
                        "VALUES (?, 'Donor V14', ?, 'hash', 'ACTIVE')",
                donorId, "donor14_" + donorId + "@netra.org"
        );
        jdbcTemplate.update(
                "INSERT INTO users (id, full_name, email, password_hash, status) " +
                        "VALUES (?, 'Verifier Staff', ?, 'hash', 'ACTIVE')",
                verifierId, "verifier14_" + verifierId + "@netra.org"
        );

        // 2. Insert blood bank
        jdbcTemplate.update(
                "INSERT INTO blood_banks (id, name, registration_number, phone, email, address, city, state, postal_code, latitude, longitude, operating_status, verification_status) " +
                        "VALUES (?, 'V14 Bank', 'REG-V14', '1234567890', 'v14@bank.org', '123 St', 'Kolkata', 'WB', '700001', 22.57, 88.36, 'OPEN', 'VERIFIED')",
                bankId
        );

        // 3. Insert blood request
        jdbcTemplate.update(
                "INSERT INTO blood_requests (id, requester_user_id, blood_group, units_required, urgency, status, hospital_name, hospital_address, city, state, postal_code, latitude, longitude, required_by) " +
                        "VALUES (?, ?, 'O+', 1, 'NORMAL', 'OPEN', 'City Hosp', 'Street', 'Kolkata', 'WB', '700001', 22.57, 88.36, CURRENT_TIMESTAMP + INTERVAL '1 day')",
                requestId, verifierId
        );

        // 4. Insert donation event
        jdbcTemplate.update(
                "INSERT INTO donation_events (id, blood_bank_id, title, event_type, status, venue_name, address, city, state, postal_code, latitude, longitude, start_at, end_at, registration_open_at, registration_close_at, donor_capacity, created_by) " +
                        "VALUES (?, ?, 'V14 Camp', 'BLOOD_DONATION_CAMP', 'PUBLISHED', 'Camp Hall', 'St', 'Kolkata', 'WB', '700001', 22.57, 88.36, CURRENT_TIMESTAMP + INTERVAL '2 days', CURRENT_TIMESTAMP + INTERVAL '3 days', CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP + INTERVAL '1 day', 50, ?)",
                eventId, bankId, verifierId
        );

        // 5. Insert valid BLOOD_REQUEST donation and verify default UUID and status
        jdbcTemplate.update(
                "INSERT INTO donations (donor_user_id, source_type, blood_request_id, donation_date) " +
                        "VALUES (?, 'BLOOD_REQUEST', ?, CURRENT_DATE)",
                donorId, requestId
        );

        Map<String, Object> reqDonation = jdbcTemplate.queryForMap(
                "SELECT id, donor_user_id, source_type, blood_request_id, donation_event_id, verification_status, version FROM donations WHERE donor_user_id = ? AND blood_request_id = ?",
                donorId, requestId
        );
        assertNotNull(reqDonation.get("id"), "PostgreSQL gen_random_uuid() must auto-populate donation id");
        assertEquals("BLOOD_REQUEST", reqDonation.get("source_type"));
        assertEquals("PENDING_VERIFICATION", reqDonation.get("verification_status"));
        assertEquals(0L, ((Number) reqDonation.get("version")).longValue());
        assertNull(reqDonation.get("donation_event_id"));

        // 6. Insert valid DONATION_EVENT donation
        jdbcTemplate.update(
                "INSERT INTO donations (donor_user_id, source_type, donation_event_id, donation_date) " +
                        "VALUES (?, 'DONATION_EVENT', ?, CURRENT_DATE)",
                donorId, eventId
        );

        Map<String, Object> evtDonation = jdbcTemplate.queryForMap(
                "SELECT id, donor_user_id, source_type, donation_event_id, verification_status FROM donations WHERE donor_user_id = ? AND donation_event_id = ?",
                donorId, eventId
        );
        assertNotNull(evtDonation.get("id"));
        assertEquals("DONATION_EVENT", evtDonation.get("source_type"));
        assertEquals("PENDING_VERIFICATION", evtDonation.get("verification_status"));

        // 7. Duplicate BLOOD_REQUEST donation for same donor must violate uq_donations_donor_blood_request
        assertThrows(DataIntegrityViolationException.class, () ->
                jdbcTemplate.update(
                        "INSERT INTO donations (donor_user_id, source_type, blood_request_id, donation_date) " +
                                "VALUES (?, 'BLOOD_REQUEST', ?, CURRENT_DATE)",
                        donorId, requestId
                )
        );

        // 8. Duplicate DONATION_EVENT donation for same donor must violate uq_donations_donor_event
        assertThrows(DataIntegrityViolationException.class, () ->
                jdbcTemplate.update(
                        "INSERT INTO donations (donor_user_id, source_type, donation_event_id, donation_date) " +
                                "VALUES (?, 'DONATION_EVENT', ?, CURRENT_DATE)",
                        donorId, eventId
                )
        );

        // 9. Source reference check: BLOOD_REQUEST with null blood_request_id must violate chk_donations_source_references
        assertThrows(DataIntegrityViolationException.class, () ->
                jdbcTemplate.update(
                        "INSERT INTO donations (donor_user_id, source_type, donation_date) " +
                                "VALUES (?, 'BLOOD_REQUEST', CURRENT_DATE)",
                        donorId
                )
        );

        // 10. Source reference check: DONATION_EVENT with blood_request_id must violate chk_donations_source_references
        assertThrows(DataIntegrityViolationException.class, () ->
                jdbcTemplate.update(
                        "INSERT INTO donations (donor_user_id, source_type, donation_event_id, blood_request_id, donation_date) " +
                                "VALUES (?, 'DONATION_EVENT', ?, ?, CURRENT_DATE)",
                        donorId, eventId, requestId
                )
        );

        // 11. Invalid source type must violate chk_donations_source_type
        assertThrows(DataIntegrityViolationException.class, () ->
                jdbcTemplate.update(
                        "INSERT INTO donations (donor_user_id, source_type, blood_request_id, donation_date) " +
                                "VALUES (?, 'COMMUNITY_DRIVE', ?, CURRENT_DATE)",
                        donorId, requestId
                )
        );

        // 12. Invalid verification status must violate chk_donations_verification_status
        assertThrows(DataIntegrityViolationException.class, () ->
                jdbcTemplate.update(
                        "INSERT INTO donations (donor_user_id, source_type, blood_request_id, donation_date, verification_status) " +
                                "VALUES (?, 'BLOOD_REQUEST', ?, CURRENT_DATE, 'APPROVED')",
                        donorId, requestId
                )
        );

        // 13. Verify notifications table accepts new donation notification types
        UUID notifId1 = UUID.randomUUID();
        UUID notifId2 = UUID.randomUUID();
        UUID notifId3 = UUID.randomUUID();

        jdbcTemplate.update(
                "INSERT INTO notifications (id, recipient_user_id, type, title, body) VALUES (?, ?, 'DONATION_SUBMITTED', 'Submitted', 'Body')",
                notifId1, donorId
        );
        jdbcTemplate.update(
                "INSERT INTO notifications (id, recipient_user_id, type, title, body) VALUES (?, ?, 'DONATION_VERIFIED', 'Verified', 'Body')",
                notifId2, donorId
        );
        jdbcTemplate.update(
                "INSERT INTO notifications (id, recipient_user_id, type, title, body) VALUES (?, ?, 'DONATION_REJECTED', 'Rejected', 'Body')",
                notifId3, donorId
        );

        Integer notifCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM notifications WHERE id IN (?, ?, ?)",
                Integer.class, notifId1, notifId2, notifId3
        );
        assertEquals(3, notifCount, "All 3 new donation notification types must be successfully inserted");
    }
}
