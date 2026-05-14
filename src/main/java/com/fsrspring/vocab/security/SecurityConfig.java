package com.fsrspring.vocab.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final String frontendBaseUrl;

    public SecurityConfig(
            CustomOAuth2UserService customOAuth2UserService,
            @Value("${app.frontend.base-url:/}") String frontendBaseUrl) {
        this.customOAuth2UserService = customOAuth2UserService;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Disabled CSRF since we might have API clients later, or modify this depending on frontend config
            .csrf(csrf -> csrf.disable())
                        .exceptionHandling(e -> e.authenticationEntryPoint(
                new org.springframework.security.web.authentication.HttpStatusEntryPoint(org.springframework.http.HttpStatus.UNAUTHORIZED)
            ))
            .authorizeHttpRequests(authz -> authz
                // Public read-only vocabulary endpoints
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/words", "/api/words/**").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/sets", "/api/sets/**").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/topics", "/api/topics/**").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/flashcards", "/api/flashcards/sources").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/flashcards/import").permitAll()
                // Dictionary lookup & enrichment (admin operations, open for internal/seed use)
                .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/dictionary/**").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/dictionary/enrich-all").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/dictionary/enrich/**").permitAll()
                // Quiz can be played as a guest; authenticated users still get progress tracking.
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/quiz/start").permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.POST, "/api/quiz/session/**").permitAll()
                // Everything else requires authentication
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll()
            )
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)
                )
                .defaultSuccessUrl(frontendBaseUrl, true)
            )
            .logout(logout -> logout
                .logoutSuccessUrl(frontendBaseUrl)
                .permitAll()
            );
        return http.build();
    }
}
