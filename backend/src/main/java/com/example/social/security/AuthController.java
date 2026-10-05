package com.example.social.security;

import com.example.social.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final String REFRESH_COOKIE = "refresh_token";

    private final AuthService authService;
    private final long refreshTokenDays;
    private final boolean secureCookie;

    public AuthController(AuthService authService,
            @Value("${app.jwt.refresh-token-days}") long refreshTokenDays,
            @Value("${app.cookie.secure}") boolean secureCookie) {
        this.authService = authService;
        this.refreshTokenDays = refreshTokenDays;
        this.secureCookie = secureCookie;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthDtos.AuthResponse>> register(@Valid @RequestBody AuthDtos.RegisterRequest request) {
        return sessionResponse(authService.register(request), 201, "Account created");
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthDtos.AuthResponse>> login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        return sessionResponse(authService.login(request), 200, "Signed in");
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthDtos.AuthResponse>> refresh(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        return sessionResponse(authService.refresh(refreshToken), 200, "Session refreshed");
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie("", 0).toString())
                .body(ApiResponse.success(200, "Signed out", null));
    }

    private ResponseEntity<ApiResponse<AuthDtos.AuthResponse>> sessionResponse(
            AuthService.AuthenticatedSession session, int status, String message) {
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, refreshCookie(session.refreshToken(), refreshTokenDays * 86400).toString())
                .body(ApiResponse.success(status, message, session.response()));
    }

    private ResponseCookie refreshCookie(String value, long maxAgeSeconds) {
        return ResponseCookie.from(REFRESH_COOKIE, value)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Lax")
                .path("/api/auth")
                .maxAge(maxAgeSeconds)
                .build();
    }
}
