package com.example.social.security;

import com.example.social.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final SecretKey signingKey;
    private final Duration accessTokenLifetime;

    public JwtService(
            @Value("${app.jwt.secret-base64}") String secretBase64,
            @Value("${app.jwt.access-token-minutes}") long accessTokenMinutes) {
        byte[] key = Decoders.BASE64.decode(secretBase64);
        if (key.length < 32) {
            throw new IllegalArgumentException("JWT signing key must be at least 256 bits");
        }
        signingKey = Keys.hmacShaKeyFor(key);
        accessTokenLifetime = Duration.ofMinutes(accessTokenMinutes);
    }

    public String createAccessToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenLifetime)))
                .signWith(signingKey)
                .compact();
    }

    public Claims parseAccessToken(String token) {
        return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
    }

    public long getAccessTokenLifetimeSeconds() {
        return accessTokenLifetime.toSeconds();
    }
}
