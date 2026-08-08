package com.talentgraph.auth.service;

import com.talentgraph.auth.User;
import com.talentgraph.auth.UserRepository;
import com.talentgraph.auth.dto.*;
import com.talentgraph.common.exception.DuplicateResourceException;
import com.talentgraph.organization.*;
import lombok.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthenticationService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    @Autowired
    public AuthenticationService(
            UserRepository userRepository,
            OrganizationRepository organizationRepository,
            OrganizationMemberRepository organizationMemberRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService
    ) {
        this.userRepository = userRepository;
        this.organizationRepository = organizationRepository;
        this.organizationMemberRepository = organizationMemberRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @Value
    public static class AuthResult {
        AuthResponse authResponse;
        String rawRefreshToken;
    }

    @Transactional
    public AuthResult register(RegisterRequest request, String ipAddress, String userAgent) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new DuplicateResourceException("Email already registered: " + normalizedEmail);
        }

        User user = User.builder()
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .isActive(true)
                .build();

        User savedUser = userRepository.save(user);

        // Optionally provision default Organization if provided
        if (request.getOrganizationName() != null && !request.getOrganizationName().isBlank()) {
            String orgName = request.getOrganizationName().trim();
            String slug = generateSlug(orgName);

            Organization org = organizationRepository.save(Organization.builder()
                    .name(orgName)
                    .slug(slug)
                    .build());

            organizationMemberRepository.save(OrganizationMember.builder()
                    .organization(org)
                    .user(savedUser)
                    .role(OrganizationRole.OWNER)
                    .build());
        }

        return createAuthResult(savedUser, ipAddress, userAgent);
    }

    @Transactional
    public AuthResult login(LoginRequest request, String ipAddress, String userAgent) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new BadCredentialsException("Account is disabled");
        }

        return createAuthResult(user, ipAddress, userAgent);
    }

    @Transactional
    public AuthResult refresh(String rawRefreshToken, String ipAddress, String userAgent) {
        RefreshTokenService.TokenPair tokenPair = refreshTokenService.rotateRefreshToken(rawRefreshToken, ipAddress, userAgent);
        User user = tokenPair.getRefreshTokenEntity().getUser();

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());
        UserResponse userResponse = buildUserResponse(user);

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken(accessToken)
                .user(userResponse)
                .expiresIn(jwtService.getAccessTokenExpirationSeconds())
                .build();

        return new AuthResult(authResponse, tokenPair.getRawRefreshToken());
    }

    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenService.revokeRefreshToken(rawRefreshToken);
    }

    @Transactional(readOnly = true)
    public UserResponse buildUserResponse(User user) {
        List<OrganizationMember> members = organizationMemberRepository.findByUserId(user.getId());
        List<UserOrganizationMembershipResponse> memberships = members.stream()
                .map(m -> UserOrganizationMembershipResponse.builder()
                        .organizationId(m.getOrganization().getId())
                        .organizationName(m.getOrganization().getName())
                        .organizationSlug(m.getOrganization().getSlug())
                        .role(m.getRole())
                        .build())
                .collect(Collectors.toList());

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .avatarUrl(user.getAvatarUrl())
                .isActive(user.getIsActive())
                .memberships(memberships)
                .createdAt(user.getCreatedAt())
                .build();
    }

    private AuthResult createAuthResult(User user, String ipAddress, String userAgent) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail());
        RefreshTokenService.TokenPair tokenPair = refreshTokenService.createRefreshToken(user, ipAddress, userAgent);
        UserResponse userResponse = buildUserResponse(user);

        AuthResponse authResponse = AuthResponse.builder()
                .accessToken(accessToken)
                .user(userResponse)
                .expiresIn(jwtService.getAccessTokenExpirationSeconds())
                .build();

        return new AuthResult(authResponse, tokenPair.getRawRefreshToken());
    }

    private String generateSlug(String name) {
        String base = name.toLowerCase().replaceAll("[^a-z0-9]", "-").replaceAll("-+", "-").replaceAll("^-|-$", "");
        if (base.isBlank()) {
            base = "org";
        }
        return base + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
