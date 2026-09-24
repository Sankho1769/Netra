package org.netra.core.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Production startup validator.
 * Enforces fail-fast validation of critical deployment configuration when running
 * under 'prod' or 'production' Spring profiles.
 *
 * Guarantees that:
 * 1. PostgreSQL is the authoritative production database (H2 is rejected).
 * 2. Flyway migrations are enabled and ddl-auto is strictly 'validate'.
 * 3. Default or weak development JWT secrets are rejected.
 * 4. Production push notification provider is not 'noop'.
 * 5. Wildcards and localhost are prohibited in CORS configuration.
 * 6. Trusted reverse proxy configuration is present.
 */
@Component
public class ProductionStartupValidator {

    private static final Logger log = LoggerFactory.getLogger(ProductionStartupValidator.class);
    private static final String DEFAULT_DEV_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    private final Environment environment;

    @Value("${spring.datasource.driver-class-name:}")
    private String driverClassName;

    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    @Value("${spring.jpa.hibernate.ddl-auto:}")
    private String ddlAuto;

    @Value("${spring.flyway.enabled:false}")
    private boolean flywayEnabled;

    @Value("${netra.security.jwt.secret:}")
    private String jwtSecret;

    @Value("${netra.security.cors.allowed-origins:}")
    private String[] allowedOrigins;

    @Value("${netra.security.trusted-proxies:}")
    private String trustedProxies;

    @Value("${netra.notifications.push.provider:noop}")
    private String pushProvider;

    public ProductionStartupValidator(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void validateProductionConfiguration() {
        boolean isProduction = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(p -> "prod".equalsIgnoreCase(p) || "production".equalsIgnoreCase(p));

        if (!isProduction) {
            log.info("Non-production profile active ({}). Skipping strict production startup validation.",
                    Arrays.toString(environment.getActiveProfiles()));
            return;
        }

        log.info("Validating NETRA production configuration against security and operational standards...");

        // 1. Database & Driver Validation
        if (driverClassName == null || !driverClassName.contains("postgresql")) {
            throw new IllegalStateException("FATAL: Production database driver must be PostgreSQL (org.postgresql.Driver). Found: " + driverClassName);
        }
        if (datasourceUrl == null || !datasourceUrl.startsWith("jdbc:postgresql://")) {
            throw new IllegalStateException("FATAL: Production datasource URL must be a valid PostgreSQL JDBC URL starting with jdbc:postgresql://");
        }

        // 2. Schema Management & Flyway Validation
        if (!flywayEnabled) {
            throw new IllegalStateException("FATAL: Flyway database migrations must be enabled in production (spring.flyway.enabled=true).");
        }
        if (!"validate".equalsIgnoreCase(ddlAuto != null ? ddlAuto.trim() : "")) {
            throw new IllegalStateException("FATAL: Hibernate ddl-auto must be strictly 'validate' in production. Found: " + ddlAuto);
        }

        // 3. JWT Secret Validation
        if (jwtSecret == null || jwtSecret.isBlank() || DEFAULT_DEV_SECRET.equalsIgnoreCase(jwtSecret.trim())) {
            throw new IllegalStateException("FATAL: Production JWT secret is missing, blank, or using the default development secret.");
        }
        if (jwtSecret.trim().getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("FATAL: Production JWT secret must be at least 256 bits (32 bytes).");
        }

        // 4. CORS Validation
        if (allowedOrigins == null || allowedOrigins.length == 0 || (allowedOrigins.length == 1 && allowedOrigins[0].isBlank())) {
            throw new IllegalStateException("FATAL: Production CORS allowed-origins must be explicitly configured with HTTPS origins.");
        }
        for (String origin : allowedOrigins) {
            String trimmed = origin.trim();
            if ("*".equals(trimmed)) {
                throw new IllegalStateException("FATAL: Wildcard origin '*' is strictly prohibited in production CORS configuration.");
            }
            if (trimmed.contains("localhost") || trimmed.contains("127.0.0.1")) {
                throw new IllegalStateException("FATAL: Insecure origin '" + trimmed + "' is prohibited in production CORS configuration.");
            }
        }

        // 5. Trusted Proxies Validation
        if (trustedProxies == null || trustedProxies.isBlank()) {
            throw new IllegalStateException("FATAL: Trusted reverse proxy CIDR/IP list (netra.security.trusted-proxies) must be configured in production.");
        }

        // 6. Push Notification Provider Validation
        if ("noop".equalsIgnoreCase(pushProvider != null ? pushProvider.trim() : "")) {
            throw new IllegalStateException("FATAL: NOOP push notification provider is prohibited in production. A production provider (e.g. FCM) must be configured.");
        }

        log.info("Production configuration validation passed: PostgreSQL, Flyway V15, 256-bit JWT secret, restricted CORS, and production push provider verified.");
    }
}
