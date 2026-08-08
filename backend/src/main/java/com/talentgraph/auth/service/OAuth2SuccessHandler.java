package com.talentgraph.auth.service;

import com.talentgraph.auth.User;
import com.talentgraph.auth.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final String frontendUrl;

    public OAuth2SuccessHandler(
            UserRepository userRepository,
            JwtService jwtService,
            RefreshTokenService refreshTokenService,
            @Value("${frontend.url:http://localhost:5173}") String frontendUrl
    ) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
        this.frontendUrl = frontendUrl;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String subStr = (String) oAuth2User.getAttributes().get("sub");
        String email = (String) oAuth2User.getAttributes().get("email");

        User user = null;
        if (subStr != null) {
            try {
                user = userRepository.findById(UUID.fromString(subStr)).orElse(null);
            } catch (IllegalArgumentException ignored) {}
        }
        if (user == null && email != null) {
            user = userRepository.findByEmail(email.trim().toLowerCase())
                    .orElseThrow(() -> new ServletException("User not found after OAuth authentication"));
        }

        if (user == null) {
            throw new ServletException("Unable to resolve user after OAuth login");
        }

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());
        RefreshTokenService.TokenPair tokenPair = refreshTokenService.createRefreshToken(user, request.getRemoteAddr(), request.getHeader("User-Agent"));

        Cookie cookie = new Cookie("talentgraph_refresh_token", tokenPair.getRawRefreshToken());
        cookie.setHttpOnly(true);
        cookie.setSecure(request.isSecure());
        cookie.setPath("/api/v1/auth");
        cookie.setMaxAge(7 * 24 * 60 * 60);
        response.addCookie(cookie);

        response.sendRedirect(frontendUrl + "/auth/callback?token=" + accessToken);
    }
}
