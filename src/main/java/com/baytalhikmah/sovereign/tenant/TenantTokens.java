package com.baytalhikmah.sovereign.tenant;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

/**
 * Signed tenant tokens (JSON Web Tokens, HMAC-SHA256). In production an identity provider issues them;
 * here {@code issue} plays that part, and {@code tenantOf} is what the application checks on every request.
 */
@Component
@Profile("tenancy")
public class TenantTokens {

    private final byte[] secret;

    private final ShopSchemas shopNames = new ShopSchemas();

    public TenantTokens(@Value("${sovereign.tenancy.token-secret}") String secret) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        if (this.secret.length < 32) {
            throw new IllegalArgumentException("sovereign.tenancy.token-secret must be at least 32 bytes");
        }
    }

    public String issue(String user, String tenant, Duration validFor) {
        try {
            Instant now = Instant.now();
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(user)
                .claim("tenant", tenant)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plus(validFor)))
                .build();
            SignedJWT token = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            token.sign(new MACSigner(secret));
            return token.serialize();
        }
        catch (JOSEException failure) {
            throw new IllegalStateException("Could not sign the token", failure);
        }
    }

    /** The tenant of a valid token; empty when the token is malformed, forged, expired or names no valid shop. */
    public Optional<String> tenantOf(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            if (!JWSAlgorithm.HS256.equals(jwt.getHeader().getAlgorithm()) || !jwt.verify(new MACVerifier(secret))) {
                return Optional.empty();
            }
            JWTClaimsSet claims = jwt.getJWTClaimsSet();
            Date expires = claims.getExpirationTime();
            if (expires == null || expires.toInstant().isBefore(Instant.now())) {
                return Optional.empty();
            }
            String tenant = claims.getStringClaim("tenant");
            shopNames.schemaName(tenant);
            return Optional.of(tenant);
        }
        catch (ParseException | JOSEException | IllegalArgumentException invalid) {
            return Optional.empty();
        }
    }

}
