package com.talentgraph.auth.service;

import com.talentgraph.auth.User;
import com.talentgraph.auth.UserRepository;
import com.talentgraph.common.exception.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
            throw new UnauthorizedException("User is not authenticated");
        }

        if (authentication.getPrincipal() instanceof User user) {
            return user;
        }

        if (authentication.getPrincipal() instanceof UUID userId) {
            return userRepository.findById(userId)
                    .orElseThrow(() -> new UnauthorizedException("Authenticated user no longer exists"));
        }

        String principalName = authentication.getName();
        try {
            UUID userId = UUID.fromString(principalName);
            return userRepository.findById(userId)
                    .orElseThrow(() -> new UnauthorizedException("Authenticated user no longer exists"));
        } catch (IllegalArgumentException e) {
            return userRepository.findByEmail(principalName.trim().toLowerCase())
                    .orElseThrow(() -> new UnauthorizedException("Authenticated user no longer exists"));
        }
    }
}
