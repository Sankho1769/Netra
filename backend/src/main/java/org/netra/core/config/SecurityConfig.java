package org.netra.core.config;

import org.netra.core.security.JwtAuthenticationFilter;
import org.netra.core.security.JwtTokenProvider;
import org.netra.features.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final List<String> allowedOrigins;

    public SecurityConfig(
            JwtTokenProvider jwtTokenProvider,
            UserRepository userRepository,
            @Value("${netra.security.cors.allowed-origins:http://localhost:3000,http://localhost:8080,http://localhost:5000}") String[] allowedOrigins) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
        this.allowedOrigins = Arrays.asList(allowedOrigins);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(headers -> headers
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'; frame-ancestors 'none';"))
                .referrerPolicy(referrer -> referrer.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
            )
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.getWriter().write("{\"status\":401,\"error\":\"UNAUTHORIZED\",\"message\":\"Authentication is required to access this resource\",\"path\":\"" + request.getRequestURI() + "\"}");
                })
            )
            .authorizeHttpRequests(auth -> auth
                // Public Authentication Endpoints
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/register").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/refresh").permitAll()

                // Public Eligibility Discovery and Session Start
                .requestMatchers(HttpMethod.GET, "/api/v1/eligibility/rules/version").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/eligibility/questions").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/eligibility/sessions").permitAll()

                // Session-specific actions: permitted at transport level for anonymous capability tokens,
                // and strictly gated at service level by Capability Token / JWT ownership
                .requestMatchers(HttpMethod.POST, "/api/v1/eligibility/sessions/*/answers").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/eligibility/sessions/*/check").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/eligibility/sessions/*/result").permitAll()

                // Blood Bank Discovery (Public GET)
                .requestMatchers(HttpMethod.GET, "/api/v1/bloodbanks").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/bloodbanks/**").permitAll()

                // Blood Bank Admin Actions
                .requestMatchers(HttpMethod.POST, "/api/v1/bloodbanks").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/bloodbanks/*/verification").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/bloodbanks/*/accounts/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/bloodbanks/*/accounts/**").hasRole("ADMIN")

                // Blood Bank Management (Coarse transport check; Service layer strictly enforces Resource Ownership)
                .requestMatchers(HttpMethod.PUT, "/api/v1/bloodbanks/*/inventory").hasAnyRole("ADMIN", "BLOODBANK")
                .requestMatchers(HttpMethod.PUT, "/api/v1/bloodbanks/**").hasAnyRole("ADMIN", "BLOODBANK")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/bloodbanks/*/status").hasAnyRole("ADMIN", "BLOODBANK")

                // Donation Events Registration & Private Endpoints
                .requestMatchers(HttpMethod.GET, "/api/v1/donation-events/my-registrations").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/v1/donation-events/*/registration").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/v1/donation-events/*/registrations").hasAnyRole("ADMIN", "BLOODBANK")
                .requestMatchers(HttpMethod.POST, "/api/v1/donation-events/*/register").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/v1/donation-events/*/registration").authenticated()

                // Donation Events Staff / Admin Endpoints
                .requestMatchers(HttpMethod.PATCH, "/api/v1/donation-events/*/approval").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/v1/donation-events").hasAnyRole("ADMIN", "BLOODBANK")
                .requestMatchers(HttpMethod.PUT, "/api/v1/donation-events/*").hasAnyRole("ADMIN", "BLOODBANK")
                .requestMatchers(HttpMethod.POST, "/api/v1/donation-events/*/submit").hasAnyRole("ADMIN", "BLOODBANK")
                .requestMatchers(HttpMethod.POST, "/api/v1/donation-events/*/cancel").hasAnyRole("ADMIN", "BLOODBANK")

                // Donation Events Discovery (Public GET)
                .requestMatchers(HttpMethod.GET, "/api/v1/donation-events").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/donation-events/nearby").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/donation-events/*").permitAll()

                // Blood Requests Endpoints
                .requestMatchers(HttpMethod.GET, "/api/v1/blood-requests/me").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/v1/blood-requests").authenticated()
                .requestMatchers(HttpMethod.PATCH, "/api/v1/blood-requests/*").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/v1/blood-requests/*/cancel").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/v1/blood-requests/*/matches").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/v1/blood-requests/*/matches").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/v1/blood-requests/*/match-responses").authenticated()

                // Donor Match Response Endpoints
                .requestMatchers(HttpMethod.GET, "/api/v1/donor/matches").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/v1/donor/matches/*").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/v1/donor/matches/*/accept").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/v1/donor/matches/*/decline").authenticated()

                .requestMatchers(HttpMethod.GET, "/api/v1/blood-requests").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/blood-requests/nearby").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/blood-requests/*").permitAll()

                // Emergency Mode Endpoints
                .requestMatchers(HttpMethod.POST, "/api/v1/emergency/blood-requests").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/v1/emergency/blood-requests/*/cancel").authenticated()

                // Notifications & Device Tokens Endpoints
                .requestMatchers("/api/v1/notifications/**").authenticated()
                .requestMatchers("/api/v1/devices/**").authenticated()

                // Protected Auth and User Endpoints
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/logout").authenticated()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/logout-all").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/v1/auth/me").authenticated()
                .requestMatchers("/api/v1/users/**").authenticated()
                .requestMatchers("/api/v1/profile/**").authenticated()
                .requestMatchers("/api/v1/donor/**").authenticated()
                .requestMatchers("/api/v1/donations/**").authenticated()
                // Actuator Health and Monitoring Endpoints
                .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                .requestMatchers("/actuator/**").hasRole("ADMIN")

                .requestMatchers("/error").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider, userRepository), UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(new org.netra.core.observability.CorrelationIdFilter(), JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        if (allowedOrigins != null && !allowedOrigins.isEmpty()) {
            boolean hasWildcard = allowedOrigins.stream()
                    .map(String::trim)
                    .anyMatch(origin -> origin.equals("*"));
            if (hasWildcard) {
                throw new IllegalStateException("CORS configuration error: Wildcard origin '*' cannot be used with allowCredentials=true.");
            }
            List<String> sanitizedOrigins = allowedOrigins.stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
            configuration.setAllowedOrigins(sanitizedOrigins);
        }

        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD"));
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "X-Capability-Token",
                "X-Session-Token",
                "Idempotency-Key",
                "X-Correlation-ID",
                "X-Request-ID",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));
        configuration.setExposedHeaders(Arrays.asList("X-Capability-Token", "X-Session-Token", "X-Correlation-ID"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
