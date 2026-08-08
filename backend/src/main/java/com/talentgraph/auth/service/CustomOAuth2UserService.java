package com.talentgraph.auth.service;

import com.talentgraph.auth.User;
import com.talentgraph.auth.UserIdentity;
import com.talentgraph.auth.UserIdentityRepository;
import com.talentgraph.auth.UserRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Map;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final UserIdentityRepository userIdentityRepository;

    public CustomOAuth2UserService(UserRepository userRepository, UserIdentityRepository userIdentityRepository) {
        this.userRepository = userRepository;
        this.userIdentityRepository = userIdentityRepository;
    }

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String provider = userRequest.getClientRegistration().getRegistrationId().toUpperCase(); // e.g. GOOGLE
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String providerSubject = (String) attributes.get("sub");
        String email = (String) attributes.get("email");
        String firstName = (String) attributes.getOrDefault("given_name", "Google");
        String lastName = (String) attributes.getOrDefault("family_name", "User");
        String avatarUrl = (String) attributes.get("picture");

        if (email == null || email.isBlank()) {
            throw new OAuth2AuthenticationException("Email not provided by OAuth provider");
        }

        String normalizedEmail = email.trim().toLowerCase();

        // 1. Find or create internal User
        User user = userRepository.findByEmail(normalizedEmail).orElseGet(() ->
                userRepository.save(User.builder()
                        .email(normalizedEmail)
                        .firstName(firstName != null && !firstName.isBlank() ? firstName : "Google")
                        .lastName(lastName != null && !lastName.isBlank() ? lastName : "User")
                        .avatarUrl(avatarUrl)
                        .isActive(true)
                        .build())
        );

        // 2. Find or create UserIdentity mapping
        if (userIdentityRepository.findByProviderAndProviderSubject(provider, providerSubject).isEmpty()) {
            userIdentityRepository.save(UserIdentity.builder()
                    .user(user)
                    .provider(provider)
                    .providerSubject(providerSubject)
                    .build());
        }

        return new DefaultOAuth2User(
                Collections.emptyList(),
                Map.of(
                        "sub", user.getId().toString(),
                        "email", user.getEmail(),
                        "firstName", user.getFirstName(),
                        "lastName", user.getLastName()
                ),
                "sub"
        );
    }
}
