package com.talentgraph.auth;

import com.talentgraph.auth.dto.*;
import com.talentgraph.auth.service.AuthenticationService;
import com.talentgraph.auth.service.CurrentUserService;
import com.talentgraph.common.ApiResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationService authenticationService;
    private final CurrentUserService currentUserService;

    public static final String REFRESH_COOKIE_NAME = "talentgraph_refresh_token";

    @Autowired
    public AuthController(AuthenticationService authenticationService, CurrentUserService currentUserService) {
        this.authenticationService = authenticationService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse
    ) {
        AuthenticationService.AuthResult result = authenticationService.register(
                request,
                servletRequest.getRemoteAddr(),
                servletRequest.getHeader("User-Agent")
        );

        setRefreshCookie(servletResponse, result.getRawRefreshToken(), servletRequest.isSecure());
        return ResponseEntity.ok(ApiResponse.success(result.getAuthResponse(), "Registration successful"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse
    ) {
        AuthenticationService.AuthResult result = authenticationService.login(
                request,
                servletRequest.getRemoteAddr(),
                servletRequest.getHeader("User-Agent")
        );

        setRefreshCookie(servletResponse, result.getRawRefreshToken(), servletRequest.isSecure());
        return ResponseEntity.ok(ApiResponse.success(result.getAuthResponse(), "Login successful"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse
    ) {
        String rawRefreshToken = extractRefreshCookie(servletRequest);
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return ResponseEntity.status(401).body(ApiResponse.error("Refresh token cookie missing"));
        }

        AuthenticationService.AuthResult result = authenticationService.refresh(
                rawRefreshToken,
                servletRequest.getRemoteAddr(),
                servletRequest.getHeader("User-Agent")
        );

        setRefreshCookie(servletResponse, result.getRawRefreshToken(), servletRequest.isSecure());
        return ResponseEntity.ok(ApiResponse.success(result.getAuthResponse(), "Token refresh successful"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse
    ) {
        String rawRefreshToken = extractRefreshCookie(servletRequest);
        authenticationService.logout(rawRefreshToken);
        clearRefreshCookie(servletResponse, servletRequest.isSecure());
        return ResponseEntity.ok(ApiResponse.success(null, "Logout successful"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {
        User currentUser = currentUserService.getCurrentUser();
        UserResponse response = authenticationService.buildUserResponse(currentUser);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private String extractRefreshCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return null;
        }
        return Arrays.stream(request.getCookies())
                .filter(c -> REFRESH_COOKIE_NAME.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private void setRefreshCookie(HttpServletResponse response, String rawToken, boolean isSecure) {
        Cookie cookie = new Cookie(REFRESH_COOKIE_NAME, rawToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(isSecure);
        cookie.setPath("/api/v1/auth");
        cookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
        response.addCookie(cookie);
    }

    private void clearRefreshCookie(HttpServletResponse response, boolean isSecure) {
        Cookie cookie = new Cookie(REFRESH_COOKIE_NAME, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(isSecure);
        cookie.setPath("/api/v1/auth");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }
}
