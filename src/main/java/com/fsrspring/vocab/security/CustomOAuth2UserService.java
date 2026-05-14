package com.fsrspring.vocab.security;

import com.fsrspring.vocab.model.AppUser;
import com.fsrspring.vocab.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final AppUserRepository appUserRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String avatarUrl = oAuth2User.getAttribute("picture"); // Use the 'picture' attribute from Google
        // For Google, the ID is typically 'sub'
        String googleId = oAuth2User.getAttribute("sub");

        AppUser appUser = appUserRepository.findByEmail(email).orElseGet(() -> {
            AppUser newUser = new AppUser();
            newUser.setEmail(email);
            newUser.setGoogleId(googleId);
            newUser.setRole(AppUser.Role.USER);
            return newUser;
        });

        // Always update the name and avatar to keep it fresh
        appUser.setName(name);
        appUser.setAvatarUrl(avatarUrl);
        appUser.setLastLoginAt(LocalDateTime.now());
        
        appUserRepository.save(appUser);
        
        return oAuth2User; // We can return a Custom wrapper if we want, but OAuth2User with email attribute works too
    }
}