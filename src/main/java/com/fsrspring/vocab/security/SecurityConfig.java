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
