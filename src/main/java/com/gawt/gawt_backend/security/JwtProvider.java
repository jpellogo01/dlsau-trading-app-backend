package com.gawt.gawt_backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtProvider {

    /**
     * Signing secret from application.properties (bezkoder.app.jwtSecret).
     * The JWT_SECRET environment variable overrides it if set.
     * There is deliberately NO built-in default: a default written in the code
     * would let anyone who reads the code forge login tokens.
     */
    private final long jwtExpirationMs;
    private final SecretKey signingKey;

    /**
     * Builds and validates the signing key once, at startup. A missing or too-short
     * secret stops startup with a clear message instead of silently rejecting every
     * login. Note: Spring trims leading spaces after "=" in application.properties.
     */
    public JwtProvider(
            @Value("${JWT_SECRET:${bezkoder.app.jwtSecret:}}") String jwtSecret,
            @Value("${bezkoder.app.jwtExpirationMs:86400000}") long jwtExpirationMs) {
        this.jwtExpirationMs = jwtExpirationMs;
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException(
                    "JWT secret is missing. Set bezkoder.app.jwtSecret in application.properties "
                            + "or the JWT_SECRET environment variable.");
        }
        byte[] bytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException(
                    "JWT secret is too short (" + bytes.length + " bytes). HS256 needs at least 32 bytes.");
        }
        signingKey = Keys.hmacShaKeyFor(bytes);
    }

    private SecretKey getSigningKey() {
        return signingKey;
    }

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("role", userDetails.getAuthorities().stream()
                .map(auth -> auth.getAuthority().replace("ROLE_", ""))
                .findFirst()
                .orElse("USER"));

        return createToken(claims, userDetails.getUsername());
    }

    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.getSubject();
    }

    public String getRoleFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.get("role", String.class);
    }

    public Date getExpiryDateFromToken(String token) {
        Claims claims = getAllClaimsFromToken(token);
        return claims.getExpiration();
    }

    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Boolean isTokenValid(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    private Boolean isTokenExpired(String token) {
        Date expiry = getExpiryDateFromToken(token);
        return expiry.before(new Date());
    }
}
