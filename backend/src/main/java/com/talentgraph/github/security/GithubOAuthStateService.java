package com.talentgraph.github.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/**
 * Secure OAuth state generator and validator.
 *
 * <p>Protects against CSRF and candidate account substitution during GitHub OAuth callbacks.
 * The state token is HMAC-SHA256 signed and contains:
 * candidate ID, initiating user ID, issuance timestamp, and a random nonce.
 */
@Service
@RequiredArgsConstructor
public class GithubOAuthStateService {

    private static final long STATE_EXPIRATION_MINUTES = 15;
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final ObjectMapper objectMapper;

    @Value("${jwt.secret:dev_jwt_secret_key_change_in_production_min_32_bytes_long!}")
    private String stateSecret;

    @Data
    @Builder
    public static class OAuthStatePayload {
        private UUID candidateId;
        private UUID actorUserId;
        private Instant issuedAt;
        private String nonce;
    }

    /**
     * Generate a signed OAuth state token for a given candidate and recruiter.
     */
    public String generateState(UUID candidateId, UUID actorUserId) {
        try {
            OAuthStatePayload payload = OAuthStatePayload.builder()
                    .candidateId(candidateId)
                    .actorUserId(actorUserId)
                    .issuedAt(Instant.now())
                    .nonce(UUID.randomUUID().toString())
                    .build();

            String jsonPayload = objectMapper.writeValueAsString(payload);
            String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(jsonPayload.getBytes(StandardCharsets.UTF_8));

            String signature = sign(encodedPayload);
            return encodedPayload + "." + signature;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate GitHub OAuth state.", e);
        }
    }

    /**
     * Validate signature and expiration of an incoming OAuth state token.
     */
    public OAuthStatePayload validateAndParseState(String stateToken) {
        if (stateToken == null || !stateToken.contains(".")) {
            throw new IllegalArgumentException("Invalid OAuth state format.");
        }

        String[] parts = stateToken.split("\\.");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Malformed OAuth state token.");
        }

        String encodedPayload = parts[0];
        String expectedSignature = parts[1];

        String actualSignature = sign(encodedPayload);
        if (!actualSignature.equals(expectedSignature)) {
            throw new IllegalArgumentException("Invalid OAuth state signature — potential CSRF attempt.");
        }

        try {
            byte[] decodedJson = Base64.getUrlDecoder().decode(encodedPayload);
            OAuthStatePayload payload = objectMapper.readValue(decodedJson, OAuthStatePayload.class);

            if (payload.getIssuedAt() == null ||
                    payload.getIssuedAt().plusSeconds(STATE_EXPIRATION_MINUTES * 60).isBefore(Instant.now())) {
                throw new IllegalArgumentException("OAuth state token has expired.");
            }

            return payload;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse OAuth state token payload.", e);
        }
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(stateSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(keySpec);
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hmacBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute OAuth state signature.", e);
        }
    }
}
