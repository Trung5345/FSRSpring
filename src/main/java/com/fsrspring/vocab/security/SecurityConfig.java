package com.fsrspring.vocab.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final String frontendBaseUrl;

    // Chỉ inject những bean không có vòng phụ thuộc với SecurityConfig
    public SecurityConfig(
            CustomOAuth2UserService customOAuth2UserService,
            @Value("${app.frontend.base-url:/}") String frontendBaseUrl) {
        this.customOAuth2UserService = customOAuth2UserService;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    // ── OAuth2 Infrastructure Beans ──────────────────────────────────────────

    @Bean
    public HttpCookieOAuth2AuthorizationRequestRepository cookieAuthorizationRequestRepository() {
        return new HttpCookieOAuth2AuthorizationRequestRepository();
    }

    @Bean
    public OAuth2AuthenticationSuccessHandler oAuth2SuccessHandler() {
        return new OAuth2AuthenticationSuccessHandler(cookieAuthorizationRequestRepository(), frontendBaseUrl);
    }

    @Bean
    public OAuth2AuthenticationFailureHandler oAuth2FailureHandler() {
        return new OAuth2AuthenticationFailureHandler(cookieAuthorizationRequestRepository(), frontendBaseUrl);
    }

    // ── Security Filter Chain ─────────────────────────────────────────────────

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // CSRF disabled — stateless OAuth2 + API clients
            .csrf(csrf -> csrf.disable())
            .exceptionHandling(e -> e.authenticationEntryPoint(
                new org.springframework.security.web.authentication.HttpStatusEntryPoint(
                    org.springframework.http.HttpStatus.UNAUTHORIZED)
            ))
            .authorizeHttpRequests(authz -> authz
                // Public read-only vocabulary endpoints
                .requestMatchers(org.springframework.http.HttpMethod.GET,
                        "/api/words", "/api/words/**").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET,
                        "/api/sets", "/api/sets/**").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET,
                        "/api/topics", "/api/topics/**").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET,
                        "/api/flashcards", "/api/flashcards/sources").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.POST,
                        "/api/flashcards/import").permitAll()
                // Dictionary lookup & enrichment
                .requestMatchers(org.springframework.http.HttpMethod.GET,
                        "/api/dictionary/**").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.POST,
                        "/api/dictionary/enrich-all", "/api/dictionary/enrich/**").permitAll()
                // Quiz (guest play allowed)
                .requestMatchers(org.springframework.http.HttpMethod.POST,
                        "/api/quiz/start", "/api/quiz/session/**").permitAll()
                // Everything else requires authentication
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            )
            .oauth2Login(oauth2 -> oauth2
                .authorizationEndpoint(auth -> auth
                    // Cookie-based state: tránh "authorization_request_not_found"
                    // khi Railway restart hoặc app restart giữa OAuth2 flow
                    .authorizationRequestRepository(cookieAuthorizationRequestRepository())
                )
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)
                )
                .successHandler(oAuth2SuccessHandler())
                .failureHandler(oAuth2FailureHandler())
            )
            .logout(logout -> logout
                .logoutSuccessUrl(frontendBaseUrl)
                .permitAll()
            );
        return http.build();
    }

    // ── CORS ─────────────────────────────────────────────────────────────────

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList(
                "https://linguist-three.vercel.app",
                "https://*.vercel.app",
                "http://localhost:*"
        ));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
