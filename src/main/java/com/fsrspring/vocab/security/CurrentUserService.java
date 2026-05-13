package com.fsrspring.vocab.security;

import com.fsrspring.vocab.model.AppUser;
import com.fsrspring.vocab.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final AppUserRepository appUserRepository;

    public AppUser getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
            throw new RuntimeException("No authenticated user found in context");
        }

        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        String email = oauth2User.getAttribute("email");
        
        return appUserRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("User not found in database for current session"));
    }
}
