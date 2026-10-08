package com.linguaoptima.api.controller;

import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.dto.request.ForgotPasswordRequest;
import com.linguaoptima.api.dto.request.LoginRequest;
import com.linguaoptima.api.dto.request.RegisterRequest;
import com.linguaoptima.api.dto.response.TokenResponse;
import com.linguaoptima.api.service.AuthService;
import com.linguaoptima.api.service.JwtService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;

    @PostMapping("/register")
    public ResponseEntity<TokenResponse> register(
        @Valid @RequestBody RegisterRequest request,
        HttpServletResponse response
    ) {
        TokenResponse tokenResponse = authService.register(request);
        setRefreshTokenCookie(response, tokenResponse.getUser().getId().toString(), tokenResponse.getUser().getEmail());
        return ResponseEntity.ok(tokenResponse);
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(
        @Valid @RequestBody LoginRequest request,
        HttpServletResponse response
    ) {
        TokenResponse tokenResponse = authService.login(request);
        setRefreshTokenCookie(response, tokenResponse.getUser().getId().toString(), tokenResponse.getUser().getEmail());
        return ResponseEntity.ok(tokenResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
        @CookieValue(name = "refreshToken", required = false) String refreshTokenFromCookie,
        @RequestHeader(name = "X-Refresh-Token", required = false) String refreshTokenFromHeader
    ) {
        String token = refreshTokenFromCookie != null ? refreshTokenFromCookie : refreshTokenFromHeader;
        TokenResponse response = authService.refreshToken(token);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
        @CookieValue(name = "refreshToken", required = false) String refreshToken,
        HttpServletResponse response
    ) {
        authService.logout(refreshToken);
        clearRefreshTokenCookie(response);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/logout-all")
    public ResponseEntity<Void> logoutAll(
        @AuthenticationPrincipal User user,
        HttpServletResponse response
    ) {
        authService.logoutAll(user);
        clearRefreshTokenCookie(response);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok().build();
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String userId, String email) {
        String refreshToken = jwtService.generateRefreshToken(java.util.UUID.fromString(userId), email);
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
            .httpOnly(true)
            .secure(false) // Set true in production HTTPS
            .path("/api/auth")
            .maxAge(30 * 24 * 60 * 60) // 30 days
            .sameSite("Strict")
            .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
            .httpOnly(true)
            .path("/api/auth")
            .maxAge(0)
            .sameSite("Strict")
            .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
