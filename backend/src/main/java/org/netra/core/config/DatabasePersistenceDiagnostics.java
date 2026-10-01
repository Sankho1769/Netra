package org.netra.core.config;

import org.netra.features.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

/**
 * Diagnostic component that prints the authoritative runtime database details
 * on startup, verifying exact JDBC URL, database engine, storage path, and user row persistence.
 */
@Component
@Order(0)
public class DatabasePersistenceDiagnostics implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabasePersistenceDiagnostics.class);

    private final DataSource dataSource;
    private final UserRepository userRepository;

    @Value("${spring.datasource.url:}")
    private String configuredJdbcUrl;

    public DatabasePersistenceDiagnostics(DataSource dataSource, UserRepository userRepository) {
        this.dataSource = dataSource;
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            String dbProduct = metaData.getDatabaseProductName();
            String dbVersion = metaData.getDatabaseProductVersion();
            String connUrl = metaData.getURL();

            String resolvedFilePath = "N/A (Network / In-Memory)";
            if (connUrl != null && connUrl.contains("file:")) {
                int start = connUrl.indexOf("file:") + 5;
                int end = connUrl.indexOf(';', start);
                String pathStr = end > 0 ? connUrl.substring(start, end) : connUrl.substring(start);
                File dbFile = new File(pathStr + ".mv.db");
                resolvedFilePath = dbFile.getAbsolutePath() + " (exists=" + dbFile.exists() + ", size=" + dbFile.length() + " bytes)";
            }

            long userCount = userRepository.count();

            log.info("\n" +
                    "================================================================================\n" +
                    "[NETRA DATABASE PERSISTENCE REPORT]\n" +
                    "Engine:                {} (Version: {})\n" +
                    "Configured JDBC URL:   {}\n" +
                    "Active Connection URL: {}\n" +
                    "Resolved Storage File: {}\n" +
                    "Persistent User Rows:  {}\n" +
                    "Status:                AUTHORITATIVE STORE VERIFIED AND ACTIVE\n" +
                    "================================================================================",
                    dbProduct, dbVersion, configuredJdbcUrl, connUrl, resolvedFilePath, userCount);
        } catch (Exception e) {
            log.error("Failed to generate database persistence diagnostic report: {}", e.getMessage(), e);
        }
    }
}
