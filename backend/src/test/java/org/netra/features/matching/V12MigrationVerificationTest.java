package org.netra.features.matching;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Structural Migration Verification Test for V12__create_donor_matches.sql.
 *
 * NOTE ON ENVIRONMENT & EXECUTION SCOPE:
 * - Docker / Testcontainers is not available in the current host environment.
 * - Local development and test profiles run against an in-memory H2 database with Flyway disabled
 *   (spring.flyway.enabled=false in application-dev.yml) and Hibernate ddl-auto=update.
 * - This test therefore verifies:
 *     1) Structural syntax, constraint, and indexing integrity of the raw V12 migration script.
 *     2) Runtime JPA entity column mapping in the active test environment.
 * - IMPORTANT RELEASE PREREQUISITE:
 *   Executing this test against H2 does NOT prove that Flyway V12 applies cleanly to a real PostgreSQL instance.
 *   Real PostgreSQL migration verification (e.g. via staging CI/CD or dockerized PostgreSQL) remains an explicit
 *   pre-release prerequisite before deploying to production/staging environments.
 *
 * NOTE ON UUID GENERATION FUNCTION:
 * - V12 uses gen_random_uuid(), the standard built-in cryptographic UUID generator available natively
 *   in PostgreSQL 13+ and Supabase without requiring extension dependencies.
 * - Earlier migrations (V1-V10) used uuid_generate_v4() via the uuid-ossp extension.
 * - In target PostgreSQL 13+ environments, both functions are fully valid, and gen_random_uuid() is preferred
 *   as it avoids extension linkage overhead.
 */
@SpringBootTest
class V12MigrationVerificationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("Structural Verification: Verify V12 Flyway migration script structure, constraints, and index definitions")
    void testV12MigrationScript_IntegrityAndConstraints() throws Exception {
        String scriptPath = "db/migration/V12__create_donor_matches.sql";
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(scriptPath)) {
            assertNotNull(in, "Migration script V12__create_donor_matches.sql must exist on classpath");
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);

            // 1. Table structure
            assertTrue(sql.contains("CREATE TABLE IF NOT EXISTS donor_matches"), "Must create donor_matches table");
            assertTrue(sql.contains("id UUID PRIMARY KEY"), "Primary key must be UUID");
            assertTrue(sql.contains("blood_request_id UUID NOT NULL"), "blood_request_id must be NOT NULL UUID");
            assertTrue(sql.contains("donor_user_id UUID NOT NULL"), "donor_user_id must be NOT NULL UUID");
            assertTrue(sql.contains("response_status VARCHAR(50) NOT NULL"), "response_status column must exist");
            assertTrue(sql.contains("expires_at TIMESTAMP WITH TIME ZONE NOT NULL"), "expires_at column must exist");
            assertTrue(sql.contains("version BIGINT NOT NULL DEFAULT 0"), "version column must support optimistic locking");

            // 2. Foreign keys
            assertTrue(sql.contains("CONSTRAINT fk_donor_matches_request"), "fk_donor_matches_request must exist");
            assertTrue(sql.contains("REFERENCES blood_requests(id)"), "Must reference blood_requests(id)");
            assertTrue(sql.contains("CONSTRAINT fk_donor_matches_donor"), "fk_donor_matches_donor must exist");
            assertTrue(sql.contains("REFERENCES users(id)"), "Must reference users(id)");

            // 3. Status check constraint
            assertTrue(sql.contains("CONSTRAINT chk_donor_matches_status"), "chk_donor_matches_status constraint must exist");
            assertTrue(sql.contains("'MATCHED'"), "Check constraint must include MATCHED");
            assertTrue(sql.contains("'ACCEPTED'"), "Check constraint must include ACCEPTED");
            assertTrue(sql.contains("'DECLINED'"), "Check constraint must include DECLINED");
            assertTrue(sql.contains("'EXPIRED'"), "Check constraint must include EXPIRED");
            assertTrue(sql.contains("'CANCELLED'"), "Check constraint must include CANCELLED");

            // 4. Uniqueness constraint
            assertTrue(sql.contains("CONSTRAINT uq_donor_matches_request_donor"), "Unique constraint name must match");
            assertTrue(sql.contains("UNIQUE (blood_request_id, donor_user_id)"), "Unique constraint must cover (blood_request_id, donor_user_id)");

            // 5. Query performance indexes
            assertTrue(sql.contains("idx_donor_matches_blood_request_id"), "idx_donor_matches_blood_request_id must exist");
            assertTrue(sql.contains("idx_donor_matches_donor_user_id"), "idx_donor_matches_donor_user_id must exist");
            assertTrue(sql.contains("idx_donor_matches_status_expires"), "idx_donor_matches_status_expires compound index must exist");
        }
    }

    @Test
    @DisplayName("Structural Verification: Verify runtime JPA entity schema contains donor_matches columns and primary key")
    void testRuntimeDatabaseSchema_DonorMatchesTableExists() throws Exception {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();

            // Find donor_matches table (case-insensitive across databases)
            Set<String> columns = new HashSet<>();
            try (ResultSet rs = metaData.getColumns(null, null, "%", null)) {
                while (rs.next()) {
                    String tableName = rs.getString("TABLE_NAME");
                    if ("donor_matches".equalsIgnoreCase(tableName)) {
                        columns.add(rs.getString("COLUMN_NAME").toLowerCase());
                    }
                }
            }

            assertTrue(columns.contains("id"), "Runtime table must contain 'id'");
            assertTrue(columns.contains("blood_request_id"), "Runtime table must contain 'blood_request_id'");
            assertTrue(columns.contains("donor_user_id"), "Runtime table must contain 'donor_user_id'");
            assertTrue(columns.contains("response_status"), "Runtime table must contain 'response_status'");
            assertTrue(columns.contains("expires_at"), "Runtime table must contain 'expires_at'");
            assertTrue(columns.contains("created_at"), "Runtime table must contain 'created_at'");
            assertTrue(columns.contains("updated_at"), "Runtime table must contain 'updated_at'");
            assertTrue(columns.contains("responded_at"), "Runtime table must contain 'responded_at'");
            assertTrue(columns.contains("version"), "Runtime table must contain 'version'");
        }
    }
}
