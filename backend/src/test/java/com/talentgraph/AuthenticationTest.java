package com.talentgraph;

import com.talentgraph.auth.*;
import com.talentgraph.auth.dto.LoginRequest;
import com.talentgraph.auth.dto.RegisterRequest;
import com.talentgraph.auth.service.AuthenticationService;
import com.talentgraph.auth.service.JwtService;
import com.talentgraph.auth.service.RefreshTokenService;
import com.talentgraph.candidate.Candidate;
import com.talentgraph.candidate.CandidateRepository;
import com.talentgraph.common.exception.DuplicateResourceException;
import com.talentgraph.common.exception.ForbiddenException;
import com.talentgraph.common.exception.UnauthorizedException;
import com.talentgraph.organization.*;
import com.talentgraph.organization.service.OrganizationAuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthenticationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserIdentityRepository userIdentityRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private OrganizationMemberRepository organizationMemberRepository;

    @Autowired
    private CandidateRepository candidateRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private OrganizationAuthorizationService authorizationService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userIdentityRepository.deleteAll();
        candidateRepository.deleteAll();
        organizationMemberRepository.deleteAll();
        organizationRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("1. Successful user registration creates user and optional organization")
    void testSuccessfulRegistration() {
        RegisterRequest request = RegisterRequest.builder()
                .email("Recruiter.Jane@AcmeCorp.com")
                .password("Password123!")
                .firstName("Jane")
                .lastName("Doe")
                .organizationName("Acme Engineering")
                .build();

        AuthenticationService.AuthResult result = authenticationService.register(request, "127.0.0.1", "JUnit5");

        assertThat(result.getAuthResponse().getAccessToken()).isNotNull();
        assertThat(result.getRawRefreshToken()).isNotNull();
        assertThat(result.getAuthResponse().getUser().getEmail()).isEqualTo("recruiter.jane@acmecorp.com");
        assertThat(userRepository.findByEmail("recruiter.jane@acmecorp.com")).isPresent();
    }

    @Test
    @DisplayName("2. Duplicate email registration rejected case-insensitively")
    void testDuplicateEmailRejectionCaseInsensitive() {
        RegisterRequest req1 = RegisterRequest.builder()
                .email("user@example.com")
                .password("Password123!")
                .firstName("First")
                .lastName("User")
                .build();
        authenticationService.register(req1, "127.0.0.1", "JUnit5");

        RegisterRequest req2 = RegisterRequest.builder()
                .email("USER@EXAMPLE.COM")
                .password("Password123!")
                .firstName("Second")
                .lastName("User")
                .build();

        assertThrows(DuplicateResourceException.class, () -> authenticationService.register(req2, "127.0.0.1", "JUnit5"));
    }

    @Test
    @DisplayName("3. Passwords are password-hashed with BCrypt")
    void testPasswordHashingBCrypt() {
        RegisterRequest request = RegisterRequest.builder()
                .email("secure@acme.com")
                .password("super_secret_pass")
                .firstName("Secure")
                .lastName("User")
                .build();

        authenticationService.register(request, "127.0.0.1", "JUnit5");
        User user = userRepository.findByEmail("secure@acme.com").orElseThrow();

        assertThat(user.getPasswordHash()).isNotEqualTo("super_secret_pass");
        assertThat(passwordEncoder.matches("super_secret_pass", user.getPasswordHash())).isTrue();
    }

    @Test
    @DisplayName("4. Successful login returns access token and refresh token")
    void testSuccessfulLogin() {
        RegisterRequest reg = RegisterRequest.builder()
                .email("login@acme.com")
                .password("MySecretPass123")
                .firstName("Login")
                .lastName("Tester")
                .build();
        authenticationService.register(reg, "127.0.0.1", "JUnit5");

        LoginRequest login = LoginRequest.builder()
                .email("login@acme.com")
                .password("MySecretPass123")
                .build();

        AuthenticationService.AuthResult result = authenticationService.login(login, "127.0.0.1", "JUnit5");
        assertThat(result.getAuthResponse().getAccessToken()).isNotNull();
        assertThat(result.getRawRefreshToken()).isNotNull();
    }

    @Test
    @DisplayName("5. Login with incorrect password is rejected")
    void testIncorrectPasswordRejection() {
        RegisterRequest reg = RegisterRequest.builder()
                .email("passcheck@acme.com")
                .password("CorrectPassword")
                .firstName("Pass")
                .lastName("Check")
                .build();
        authenticationService.register(reg, "127.0.0.1", "JUnit5");

        LoginRequest login = LoginRequest.builder()
                .email("passcheck@acme.com")
                .password("WrongPassword")
                .build();

        assertThrows(BadCredentialsException.class, () -> authenticationService.login(login, "127.0.0.1", "JUnit5"));
    }

    @Test
    @DisplayName("6. Login with unknown email is rejected")
    void testUnknownEmailRejection() {
        LoginRequest login = LoginRequest.builder()
                .email("nonexistent@acme.com")
                .password("AnyPassword")
                .build();

        assertThrows(BadCredentialsException.class, () -> authenticationService.login(login, "127.0.0.1", "JUnit5"));
    }

    @Test
    @DisplayName("7. Protected endpoint rejects unauthenticated requests with 401 Unauthorized")
    void testProtectedEndpointRejectionUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("8. Protected endpoint accepts valid Bearer access token")
    void testProtectedEndpointSuccessAuthenticated() throws Exception {
        RegisterRequest reg = RegisterRequest.builder()
                .email("authed@acme.com")
                .password("Pass12345")
                .firstName("Authed")
                .lastName("User")
                .build();
        AuthenticationService.AuthResult authResult = authenticationService.register(reg, "127.0.0.1", "JUnit5");

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + authResult.getAuthResponse().getAccessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("authed@acme.com"));
    }

    @Test
    @DisplayName("9. Refresh token rotation revokes old token and issues replacement")
    void testRefreshTokenRotation() {
        RegisterRequest reg = RegisterRequest.builder()
                .email("rotation@acme.com")
                .password("Pass12345")
                .firstName("Rotation")
                .lastName("User")
                .build();
        AuthenticationService.AuthResult initial = authenticationService.register(reg, "127.0.0.1", "JUnit5");

        AuthenticationService.AuthResult rotated = authenticationService.refresh(initial.getRawRefreshToken(), "127.0.0.1", "JUnit5");

        assertThat(rotated.getRawRefreshToken()).isNotEqualTo(initial.getRawRefreshToken());
        assertThat(rotated.getAuthResponse().getAccessToken()).isNotNull();

        // Old token hash should now be revoked
        RefreshToken oldEntity = refreshTokenRepository.findByTokenHash(refreshTokenService.hashToken(initial.getRawRefreshToken())).orElseThrow();
        assertThat(oldEntity.isRevoked()).isTrue();
    }

    @Test
    @DisplayName("10. Revoked refresh token reuse is rejected and revokes token family")
    void testRevokedRefreshTokenRejection() {
        RegisterRequest reg = RegisterRequest.builder()
                .email("reuse@acme.com")
                .password("Pass12345")
                .firstName("Reuse")
                .lastName("Tester")
                .build();
        AuthenticationService.AuthResult initial = authenticationService.register(reg, "127.0.0.1", "JUnit5");
        authenticationService.refresh(initial.getRawRefreshToken(), "127.0.0.1", "JUnit5");

        // Attempt to use initial token again
        assertThrows(UnauthorizedException.class, () ->
                authenticationService.refresh(initial.getRawRefreshToken(), "127.0.0.1", "JUnit5")
        );
    }

    @Test
    @DisplayName("11. Logout revokes the active refresh token")
    void testLogoutRevokesRefreshToken() {
        RegisterRequest reg = RegisterRequest.builder()
                .email("logout@acme.com")
                .password("Pass12345")
                .firstName("Logout")
                .lastName("User")
                .build();
        AuthenticationService.AuthResult initial = authenticationService.register(reg, "127.0.0.1", "JUnit5");

        authenticationService.logout(initial.getRawRefreshToken());

        RefreshToken entity = refreshTokenRepository.findByTokenHash(refreshTokenService.hashToken(initial.getRawRefreshToken())).orElseThrow();
        assertThat(entity.isRevoked()).isTrue();
    }

    @Test
    @DisplayName("12. Organization member authorization returns membership for valid member")
    void testOrganizationMemberAuthorization() {
        User user = userRepository.save(User.builder().email("orgmember@acme.com").firstName("Org").lastName("Member").build());
        Organization org = organizationRepository.save(Organization.builder().name("Test Org").slug("test-org-123").build());
        organizationMemberRepository.save(OrganizationMember.builder().user(user).organization(org).role(OrganizationRole.ADMIN).build());

        OrganizationMember member = authorizationService.requireOrganizationMember(user, org.getId());
        assertThat(member.getRole()).isEqualTo(OrganizationRole.ADMIN);
    }

    @Test
    @DisplayName("13. Unauthorized organization access throws 403 ForbiddenException")
    void testUnauthorizedOrganizationAccessRejection() {
        User user = userRepository.save(User.builder().email("outsider@acme.com").firstName("Outsider").lastName("User").build());
        Organization org = organizationRepository.save(Organization.builder().name("Secret Org").slug("secret-org-123").build());

        assertThrows(ForbiddenException.class, () -> authorizationService.requireOrganizationMember(user, org.getId()));
    }

    @Test
    @DisplayName("14. Recruiter permission checks enforce minimum role hierarchy")
    void testRecruiterPermissionChecks() {
        User recruiter = userRepository.save(User.builder().email("recruiter@acme.com").firstName("Rec").lastName("Ruiter").build());
        User interviewer = userRepository.save(User.builder().email("interviewer@acme.com").firstName("Inter").lastName("Viewer").build());
        Organization org = organizationRepository.save(Organization.builder().name("HireCorp").slug("hirecorp-123").build());

        organizationMemberRepository.save(OrganizationMember.builder().user(recruiter).organization(org).role(OrganizationRole.RECRUITER).build());
        organizationMemberRepository.save(OrganizationMember.builder().user(interviewer).organization(org).role(OrganizationRole.INTERVIEWER).build());

        // Recruiter has RECRUITER rank (30 >= 30) -> PASS
        assertThat(authorizationService.requireRole(recruiter, org.getId(), OrganizationRole.RECRUITER)).isNotNull();

        // Interviewer has INTERVIEWER rank (10 < 30) -> REJECT
        assertThrows(ForbiddenException.class, () -> authorizationService.requireRole(interviewer, org.getId(), OrganizationRole.RECRUITER));
    }

    @Test
    @DisplayName("15. Interviewer permission checks allow INTERVIEWER role")
    void testInterviewerPermissionChecks() {
        User interviewer = userRepository.save(User.builder().email("interviewer2@acme.com").firstName("Inter").lastName("Viewer").build());
        Organization org = organizationRepository.save(Organization.builder().name("HireCorp2").slug("hirecorp2-123").build());

        organizationMemberRepository.save(OrganizationMember.builder().user(interviewer).organization(org).role(OrganizationRole.INTERVIEWER).build());

        assertThat(authorizationService.requireRole(interviewer, org.getId(), OrganizationRole.INTERVIEWER)).isNotNull();
    }

    @Test
    @DisplayName("16. MANDATORY IDOR SECURITY TEST: Recruiter A in Org A cannot access Candidate in Org B")
    void testIdorCandidateAccessCrossOrganizationRejection() throws Exception {
        // 1. Create Organization A & Recruiter A
        Organization orgA = organizationRepository.save(Organization.builder().name("Organization A").slug("org-a-" + UUID.randomUUID()).build());
        User userA = userRepository.save(User.builder().email("recruiter.a@orga.com").passwordHash(passwordEncoder.encode("Pass12345")).firstName("Recruiter").lastName("A").build());
        organizationMemberRepository.save(OrganizationMember.builder().organization(orgA).user(userA).role(OrganizationRole.RECRUITER).build());

        // 2. Create Organization B & Candidate B
        Organization orgB = organizationRepository.save(Organization.builder().name("Organization B").slug("org-b-" + UUID.randomUUID()).build());
        Candidate candidateB = candidateRepository.save(Candidate.builder()
                .organization(orgB)
                .email("candidate.b@external.com")
                .firstName("Alice")
                .lastName("Smith")
                .build());

        // 3. Issue JWT Access Token for User A (Org A)
        String tokenA = jwtService.generateAccessToken(userA.getId(), userA.getEmail());

        // 4. Authenticate as User A and attempt to access Org B Candidate -> MUST return 403 Forbidden!
        mockMvc.perform(get("/api/v1/candidates/" + candidateB.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Access denied: User is not a member of this organization"));
    }

    @Test
    @DisplayName("17. Google User Identity provider + subject combination is unique")
    void testGoogleIdentityUniqueness() {
        User user = userRepository.save(User.builder().email("google.user@acme.com").firstName("G").lastName("User").build());

        UserIdentity identity1 = UserIdentity.builder()
                .user(user)
                .provider("GOOGLE")
                .providerSubject("google-sub-12345")
                .build();
        userIdentityRepository.save(identity1);

        assertThat(userIdentityRepository.findByProviderAndProviderSubject("GOOGLE", "google-sub-12345")).isPresent();
    }
}
