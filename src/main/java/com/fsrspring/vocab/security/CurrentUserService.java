package com.fsrspring.vocab.security;

import com.fsrspring.vocab.model.AppUser;
import com.fsrspring.vocab.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final AppUserRepository appUserRepository;

    public AppUser getCurrentUser() {
        return getCurrentUserOptional()
            .orElseThrow(() -> new RuntimeException("No authenticated user found in context"));
    }

    public Optional<AppUser> getCurrentUserOptional() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
            return Optional.empty();
        }

        if (!(authentication.getPrincipal() instanceof OAuth2User oauth2User)) {
            return Optional.empty();
        }

        String email = oauth2User.getAttribute("email");
        
        return appUserRepository.findByEmail(email);
    }
}
