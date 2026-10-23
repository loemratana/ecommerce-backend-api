package com.example.ecommerce_api.security.jwt;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import com.example.ecommerce_api.settings.service.SettingsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;


@Component
public class JwtUtils {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    private final SecretKey key;
    private final SettingsService settingsService;

    public JwtUtils(@Value("${jwt.secret}") String jwtSecret, SettingsService settingsService) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
        this.settingsService = settingsService;
    }

    /** Set by admins in the dashboard (Settings / Security); only applies to newly issued tokens. */
    public long getAccessTokenTtlSeconds() {
        return settingsService.get().getSecurity().getAccessTokenTtlMinutes() * 60L;
    }

    public String generateAccessToken(String subjectEmail) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(subjectEmail)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(getAccessTokenTtlSeconds())))
                .signWith(key)
                .compact();
    }


    /**
     * Parses and validates the token in one pass. Never throws - returns
     * empty for anything expired, malformed, unsupported, or tampered with.
     */
    public Optional<String> extractValidSubject(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.ofNullable(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            logger.debug("Rejected access token: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
