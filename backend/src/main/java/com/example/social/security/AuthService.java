package com.example.social.security;

import com.example.social.common.error.ConflictException;
import com.example.social.common.error.UnauthorizedException;
import com.example.social.user.User;
import com.example.social.user.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final long refreshTokenDays;

    public AuthService(UserRepository users, RefreshTokenRepository refreshTokens, PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager, JwtService jwtService,
            @Value("${app.jwt.refresh-token-days}") long refreshTokenDays) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenDays = refreshTokenDays;
    }

    @Transactional
    public AuthenticatedSession register(AuthDtos.RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account with this email already exists");
        }
        User user = users.save(new User(email, request.displayName().trim(), passwordEncoder.encode(request.password())));
        return createSession(user);
    }

    @Transactional
    public AuthenticatedSession login(AuthDtos.LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().trim().toLowerCase(), request.password()));
        User user = users.findByEmailIgnoreCase(request.email().trim())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
        return createSession(user);
    }

    @Transactional
    public AuthenticatedSession refresh(String presentedToken) {
        if (presentedToken == null || presentedToken.isBlank()) {
            throw new UnauthorizedException("Refresh token is missing");
        }
        RefreshToken current = refreshTokens.findByTokenHashForUpdate(hash(presentedToken))
                .orElseThrow(() -> new UnauthorizedException("Refresh token is invalid"));
        if (current.getRevokedAt() != null || !current.getExpiresAt().isAfter(Instant.now())) {
            throw new UnauthorizedException("Refresh token is expired or revoked");
        }
        current.revoke();
        return createSession(current.getUser());
    }

    @Transactional
    public void logout(String presentedToken) {
        if (presentedToken == null || presentedToken.isBlank()) {
            return;
        }
        refreshTokens.findByTokenHashForUpdate(hash(presentedToken)).ifPresent(RefreshToken::revoke);
    }

    private AuthenticatedSession createSession(User user) {
        byte[] randomToken = new byte[48];
        RANDOM.nextBytes(randomToken);
        String refreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomToken);
        refreshTokens.save(new RefreshToken(hash(refreshToken), user,
                Instant.now().plus(refreshTokenDays, ChronoUnit.DAYS)));
        AuthDtos.AuthResponse response = new AuthDtos.AuthResponse(jwtService.createAccessToken(user), "Bearer",
                jwtService.getAccessTokenLifetimeSeconds(), user.getId(), user.getEmail(), user.getDisplayName());
        return new AuthenticatedSession(response, refreshToken);
    }

    private String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    public record AuthenticatedSession(AuthDtos.AuthResponse response, String refreshToken) {}
}
