package com.fsrspring.vocab.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

import java.io.IOException;

public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler.class);

    private final HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository;
    private final String frontendBaseUrl;

    public OAuth2AuthenticationSuccessHandler(
            HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository,
            String frontendBaseUrl) {
        this.authorizationRequestRepository = authorizationRequestRepository;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        // Xóa cookie OAuth2 state sau khi xác thực thành công
        authorizationRequestRepository.removeAuthorizationRequest(request, response);
        log.info("OAuth2 login success, redirecting to: {}", frontendBaseUrl);
        getRedirectStrategy().sendRedirect(request, response, frontendBaseUrl);
    }
}
