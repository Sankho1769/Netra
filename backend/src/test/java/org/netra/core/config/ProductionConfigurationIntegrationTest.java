package org.netra.core.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration and fail-fast validation tests for NETRA Production Configuration.
 *
 * Verifies:
 * 1. Fail-fast triggers when default dev JWT secret or weak secret is supplied in prod.
 * 2. Fail-fast triggers when non-PostgreSQL driver or non-PostgreSQL JDBC URL is supplied.
 * 3. Fail-fast triggers when Flyway is disabled or ddl-auto is not 'validate'.
 * 4. Fail-fast triggers when push notification provider is 'noop'.
 * 5. Fail-fast triggers when CORS contains wildcard '*' or 'localhost'.
 * 6. Fail-fast triggers when trusted reverse proxy list is blank.
 * 7. Successful validation when authoritative production properties are provided.
 */
class ProductionConfigurationIntegrationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(ProductionStartupValidator.class);

    private static final String STRONG_PROD_SECRET =
            "c2VjdXJlLXByb2R1Y3Rpb24tand0LXNlY3JldC1leGNlZWRpbmctMjU2LWJpdHMta2V5LXNwZWM=";

    @Test
    @DisplayName("Prod profile fails fast when JWT secret is default development secret")
    void testProdProfileFailsFastWhenJwtSecretIsDefaultDevSecret() {
        runner.withPropertyValues(
                "spring.profiles.active=prod",
                "spring.datasource.driver-class-name=org.postgresql.Driver",
                "spring.datasource.url=jdbc:postgresql://localhost:5432/netra",
                "spring.flyway.enabled=true",
                "spring.jpa.hibernate.ddl-auto=validate",
                "netra.security.jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970",
                "netra.security.cors.allowed-origins=https://netra.health",
                "netra.security.trusted-proxies=10.0.0.0/8",
                "netra.notifications.push.provider=fcm"
        ).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .rootCause()
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("default development secret");
        });
    }

    @Test
    @DisplayName("Prod profile fails fast when JWT secret is shorter than 256 bits (32 bytes)")
    void testProdProfileFailsFastWhenJwtSecretIsTooShort() {
        runner.withPropertyValues(
                "spring.profiles.active=prod",
                "spring.datasource.driver-class-name=org.postgresql.Driver",
                "spring.datasource.url=jdbc:postgresql://localhost:5432/netra",
                "spring.flyway.enabled=true",
                "spring.jpa.hibernate.ddl-auto=validate",
                "netra.security.jwt.secret=too-short-secret",
                "netra.security.cors.allowed-origins=https://netra.health",
                "netra.security.trusted-proxies=10.0.0.0/8",
                "netra.notifications.push.provider=fcm"
        ).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .rootCause()
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("at least 256 bits");
        });
    }

    @Test
    @DisplayName("Prod profile fails fast when database driver is not PostgreSQL")
    void testProdProfileFailsFastWhenDriverIsNotPostgreSql() {
        runner.withPropertyValues(
                "spring.profiles.active=prod",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.url=jdbc:postgresql://localhost:5432/netra",
                "spring.flyway.enabled=true",
                "spring.jpa.hibernate.ddl-auto=validate",
                "netra.security.jwt.secret=" + STRONG_PROD_SECRET,
                "netra.security.cors.allowed-origins=https://netra.health",
                "netra.security.trusted-proxies=10.0.0.0/8",
                "netra.notifications.push.provider=fcm"
        ).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .rootCause()
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Production database driver must be PostgreSQL");
        });
    }

    @Test
    @DisplayName("Prod profile fails fast when datasource URL is not PostgreSQL")
    void testProdProfileFailsFastWhenDatasourceUrlIsNotPostgreSql() {
        runner.withPropertyValues(
                "spring.profiles.active=prod",
                "spring.datasource.driver-class-name=org.postgresql.Driver",
                "spring.datasource.url=jdbc:h2:mem:netradb",
                "spring.flyway.enabled=true",
                "spring.jpa.hibernate.ddl-auto=validate",
                "netra.security.jwt.secret=" + STRONG_PROD_SECRET,
                "netra.security.cors.allowed-origins=https://netra.health",
                "netra.security.trusted-proxies=10.0.0.0/8",
                "netra.notifications.push.provider=fcm"
        ).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .rootCause()
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("valid PostgreSQL JDBC URL");
        });
    }

    @Test
    @DisplayName("Prod profile fails fast when Flyway is disabled")
    void testProdProfileFailsFastWhenFlywayDisabled() {
        runner.withPropertyValues(
                "spring.profiles.active=prod",
                "spring.datasource.driver-class-name=org.postgresql.Driver",
                "spring.datasource.url=jdbc:postgresql://localhost:5432/netra",
                "spring.flyway.enabled=false",
                "spring.jpa.hibernate.ddl-auto=validate",
                "netra.security.jwt.secret=" + STRONG_PROD_SECRET,
                "netra.security.cors.allowed-origins=https://netra.health",
                "netra.security.trusted-proxies=10.0.0.0/8",
                "netra.notifications.push.provider=fcm"
        ).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .rootCause()
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Flyway database migrations must be enabled");
        });
    }

    @Test
    @DisplayName("Prod profile fails fast when Hibernate ddl-auto is not validate")
    void testProdProfileFailsFastWhenDdlAutoIsNotValidate() {
        runner.withPropertyValues(
                "spring.profiles.active=prod",
                "spring.datasource.driver-class-name=org.postgresql.Driver",
                "spring.datasource.url=jdbc:postgresql://localhost:5432/netra",
                "spring.flyway.enabled=true",
                "spring.jpa.hibernate.ddl-auto=update",
                "netra.security.jwt.secret=" + STRONG_PROD_SECRET,
                "netra.security.cors.allowed-origins=https://netra.health",
                "netra.security.trusted-proxies=10.0.0.0/8",
                "netra.notifications.push.provider=fcm"
        ).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .rootCause()
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Hibernate ddl-auto must be strictly 'validate'");
        });
    }

    @Test
    @DisplayName("Prod profile fails fast when push notification provider is noop")
    void testProdProfileFailsFastWhenPushProviderIsNoop() {
        runner.withPropertyValues(
                "spring.profiles.active=prod",
                "spring.datasource.driver-class-name=org.postgresql.Driver",
                "spring.datasource.url=jdbc:postgresql://localhost:5432/netra",
                "spring.flyway.enabled=true",
                "spring.jpa.hibernate.ddl-auto=validate",
                "netra.security.jwt.secret=" + STRONG_PROD_SECRET,
                "netra.security.cors.allowed-origins=https://netra.health",
                "netra.security.trusted-proxies=10.0.0.0/8",
                "netra.notifications.push.provider=noop"
        ).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .rootCause()
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("NOOP push notification provider is prohibited");
        });
    }

    @Test
    @DisplayName("Prod profile fails fast when CORS allowed origins contains wildcard (*)")
    void testProdProfileFailsFastWhenCorsContainsWildcard() {
        runner.withPropertyValues(
                "spring.profiles.active=prod",
                "spring.datasource.driver-class-name=org.postgresql.Driver",
                "spring.datasource.url=jdbc:postgresql://localhost:5432/netra",
                "spring.flyway.enabled=true",
                "spring.jpa.hibernate.ddl-auto=validate",
                "netra.security.jwt.secret=" + STRONG_PROD_SECRET,
                "netra.security.cors.allowed-origins=*",
                "netra.security.trusted-proxies=10.0.0.0/8",
                "netra.notifications.push.provider=fcm"
        ).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .rootCause()
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Wildcard origin '*' is strictly prohibited");
        });
    }

    @Test
    @DisplayName("Prod profile fails fast when CORS allowed origins contains localhost")
    void testProdProfileFailsFastWhenCorsContainsLocalhost() {
        runner.withPropertyValues(
                "spring.profiles.active=prod",
                "spring.datasource.driver-class-name=org.postgresql.Driver",
                "spring.datasource.url=jdbc:postgresql://localhost:5432/netra",
                "spring.flyway.enabled=true",
                "spring.jpa.hibernate.ddl-auto=validate",
                "netra.security.jwt.secret=" + STRONG_PROD_SECRET,
                "netra.security.cors.allowed-origins=http://localhost:3000",
                "netra.security.trusted-proxies=10.0.0.0/8",
                "netra.notifications.push.provider=fcm"
        ).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .rootCause()
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Insecure origin 'http://localhost:3000' is prohibited");
        });
    }

    @Test
    @DisplayName("Prod profile fails fast when trusted reverse proxies is blank")
    void testProdProfileFailsFastWhenTrustedProxiesBlank() {
        runner.withPropertyValues(
                "spring.profiles.active=prod",
                "spring.datasource.driver-class-name=org.postgresql.Driver",
                "spring.datasource.url=jdbc:postgresql://localhost:5432/netra",
                "spring.flyway.enabled=true",
                "spring.jpa.hibernate.ddl-auto=validate",
                "netra.security.jwt.secret=" + STRONG_PROD_SECRET,
                "netra.security.cors.allowed-origins=https://netra.health",
                "netra.security.trusted-proxies=",
                "netra.notifications.push.provider=fcm"
        ).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .rootCause()
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Trusted reverse proxy CIDR/IP list");
        });
    }

    @Test
    @DisplayName("Prod profile successfully initializes when all production invariants are satisfied")
    void testProdProfileSucceedsWithValidProductionConfiguration() {
        runner.withPropertyValues(
                "spring.profiles.active=prod",
                "spring.datasource.driver-class-name=org.postgresql.Driver",
                "spring.datasource.url=jdbc:postgresql://db.netra.internal:5432/netra",
                "spring.flyway.enabled=true",
                "spring.jpa.hibernate.ddl-auto=validate",
                "netra.security.jwt.secret=" + STRONG_PROD_SECRET,
                "netra.security.cors.allowed-origins=https://app.netra.health,https://admin.netra.health",
                "netra.security.trusted-proxies=10.0.0.0/8,172.16.0.0/12",
                "netra.notifications.push.provider=fcm"
        ).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(ProductionStartupValidator.class);
        });
    }

    @Test
    @DisplayName("Non-prod profiles skip strict production validation checks")
    void testNonProdProfileSkipsStrictProductionValidation() {
        runner.withPropertyValues(
                "spring.profiles.active=dev",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.url=jdbc:h2:mem:netradb",
                "spring.flyway.enabled=false",
                "spring.jpa.hibernate.ddl-auto=update",
                "netra.security.jwt.secret=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970",
                "netra.security.cors.allowed-origins=http://localhost:3000",
                "netra.security.trusted-proxies=127.0.0.1",
                "netra.notifications.push.provider=noop"
        ).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(ProductionStartupValidator.class);
        });
    }
}
