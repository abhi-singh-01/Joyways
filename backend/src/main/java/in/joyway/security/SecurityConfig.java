package in.joyway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.joyway.config.CorsProperties;
import in.joyway.config.JwtProperties;
import in.joyway.util.CookieDomainNormalizer;
import in.joyway.dto.ApiResponse;
import in.joyway.observability.RequestCorrelationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CorsProperties corsProperties;
    private final JwtProperties jwtProperties;
    private final ObjectMapper objectMapper;
    private final Environment environment;

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            CookieCsrfTokenRepository csrfTokenRepository
    ) throws Exception {
        http
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                        .ignoringRequestMatchers(
                                "/api/payments/webhook",
                                "/api/bookings/**",
                                "/api/payments/**",
                                "/api/create-order",
                                "/api/verify-payment",
                                "/api/health",
                                "/api/health/**",
                                "/api/packages/**",
                                "/api/files/**",
                                "/api/contact/**",
                                "/api/admin/auth/login"
                        ))
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .headers(headers -> headers
                        .contentTypeOptions(contentType -> {})
                        .frameOptions(frame -> frame.deny())
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                        .httpStrictTransportSecurity(hsts -> hsts
                                .includeSubDomains(true)
                                .maxAgeInSeconds(31536000))
                        .contentSecurityPolicy(csp -> csp
                                .policyDirectives("default-src 'none'; frame-ancestors 'none'; base-uri 'none'"))
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers("/api/health", "/api/health/**").permitAll()
                            .requestMatchers(HttpMethod.GET, "/api/packages", "/api/packages/**").permitAll()
                            .requestMatchers(HttpMethod.GET, "/api/bookings/*").permitAll()
                            .requestMatchers(HttpMethod.GET, "/api/bookings/*/receipt").permitAll()
                            .requestMatchers(HttpMethod.POST, "/api/bookings", "/api/bookings/**").permitAll()
                            .requestMatchers(HttpMethod.POST, "/api/payments/orders", "/api/payments/verify").permitAll()
                            .requestMatchers(HttpMethod.POST, "/api/create-order", "/api/verify-payment").permitAll()
                            .requestMatchers(HttpMethod.POST, "/api/payments/webhook").permitAll()
                            .requestMatchers(HttpMethod.POST, "/api/contact/enquiry").permitAll()
                            .requestMatchers(HttpMethod.GET, "/api/files/package-images/**").permitAll()
                            .requestMatchers("/api/admin/auth/login").permitAll();
                    if (isDevProfile()) {
                        auth.requestMatchers("/api/dev/**").permitAll();
                    }
                    auth.requestMatchers("/api/admin/**").hasRole("ADMIN")
                            .anyRequest().authenticated();
                })
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) ->
                                writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Authentication required."))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                writeError(response, HttpServletResponse.SC_FORBIDDEN, "Access denied."))
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        String errorCode = status == HttpServletResponse.SC_UNAUTHORIZED
                ? "AUTHENTICATION_REQUIRED"
                : "ACCESS_DENIED";
        objectMapper.writeValue(response.getOutputStream(), ApiResponse.error(message, errorCode));
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        List<String> origins = Arrays.stream(corsProperties.getAllowedOrigins().split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "Origin",
                "X-Requested-With",
                "X-XSRF-TOKEN",
                RequestCorrelationFilter.REQUEST_ID_HEADER
        ));
        configuration.setExposedHeaders(List.of(
                "Content-Type",
                RequestCorrelationFilter.REQUEST_ID_HEADER
        ));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    private boolean isDevProfile() {
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> profile.equalsIgnoreCase("dev")
                        || profile.equalsIgnoreCase("development"));
    }

    @Bean
    public CookieCsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookiePath("/");
        repository.setCookieCustomizer(cookie -> {
            String domain = CookieDomainNormalizer.forResponseCookie(jwtProperties.getCookieDomain());
            if (domain != null && !domain.isBlank()) {
                cookie.domain(domain);
            }
            cookie.sameSite("Lax");
            if (jwtProperties.isCookieSecure()) {
                cookie.secure(true);
            }
        });
        return repository;
    }
}
