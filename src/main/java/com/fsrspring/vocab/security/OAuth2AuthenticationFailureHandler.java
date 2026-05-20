package com.fsrspring.vocab.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2AuthenticationFailureHandler.class);

    private final HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository;
    private final String frontendBaseUrl;

    public OAuth2AuthenticationFailureHandler(
            HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository,
            String frontendBaseUrl) {
        this.authorizationRequestRepository = authorizationRequestRepository;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        // Xóa cookie OAuth2 state
        authorizationRequestRepository.removeAuthorizationRequest(request, response);

        String errorMessage = URLEncoder.encode(exception.getMessage(), StandardCharsets.UTF_8);
        log.error("OAuth2 authentication failed: {}", exception.getMessage());

        // Redirect về frontend với error query param thay vì /login?error (404)
        String targetUrl = UriComponentsBuilder.fromUriString(frontendBaseUrl)
                .queryParam("error", errorMessage)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
