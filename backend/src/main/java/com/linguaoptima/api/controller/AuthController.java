/**
 * @file AuthController.java
 * @brief REST controller managing user authentication, registration, and tokens.
 */
package com.linguaoptima.api.controller;

import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.dto.request.ForgotPasswordRequest;
import com.linguaoptima.api.dto.request.GoogleAuthRequest;
import com.linguaoptima.api.dto.request.LoginRequest;
import com.linguaoptima.api.dto.request.RegisterRequest;
import com.linguaoptima.api.dto.response.TokenResponse;
import com.linguaoptima.api.service.AuthService;
import com.linguaoptima.api.service.JwtService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * @brief REST controller managing user authentication, registration, and tokens.
 *
 * Implements JWT access token generation, Google OAuth2 sign-in, HttpOnly cookie refresh token rotation,
 * multi-device logout, and password recovery workflows.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    /** @brief Field representing auth service in AuthController. */
    private final AuthService authService;
    /** @brief Field representing jwt service in AuthController. */
    private final JwtService jwtService;

    /**
     * @brief Registers a new user and sets a secure HttpOnly refresh token cookie.
     *
     * @param request User registration payload.
     * @param response HTTP servlet response for setting cookies.
     * @return HTTP 200 with TokenResponse containing access token and user details.
     */
    @PostMapping("/register")
    public ResponseEntity<TokenResponse> register(
        @Valid @RequestBody RegisterRequest request,
        HttpServletResponse response
    ) {
        TokenResponse tokenResponse = authService.register(request);
        setRefreshTokenCookie(response, tokenResponse);
        return ResponseEntity.ok(tokenResponse);
    }

    /**
     * @brief Authenticates user credentials and returns session tokens.
     *
     * @param request Credentials containing email and plaintext password.
     * @param response HTTP servlet response for setting refresh cookie.
     * @return HTTP 200 with TokenResponse containing access token and user details.
     */
    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(
        @Valid @RequestBody LoginRequest request,
        HttpServletResponse response
    ) {
        TokenResponse tokenResponse = authService.login(request);
        setRefreshTokenCookie(response, tokenResponse);
        return ResponseEntity.ok(tokenResponse);
    }

    /**
     * @brief Authenticates or registers a user via Google OAuth2 ID token and sets a refresh cookie.
     *
     * @param request Google OAuth2 payload containing the ID token and optional role for new accounts.
     * @param response HTTP servlet response for setting the HttpOnly refresh cookie.
     * @return HTTP 200 with TokenResponse containing access token and user details.
     */
    @PostMapping("/google")
    public ResponseEntity<TokenResponse> googleLogin(
        @Valid @RequestBody GoogleAuthRequest request,
        HttpServletResponse response
    ) {
        TokenResponse tokenResponse = authService.googleLogin(request);
        setRefreshTokenCookie(response, tokenResponse);
        return ResponseEntity.ok(tokenResponse);
    }


    /**
     * @brief Refreshes an expired access token using the HttpOnly refresh token.
     *
     * @param refreshTokenFromCookie Refresh token extracted from cookie.
     * @param refreshTokenFromHeader Optional refresh token from fallback header.
     * @return HTTP 200 with new TokenResponse.
     */
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(
        @CookieValue(name = "refreshToken", required = false) String refreshTokenFromCookie,
        @RequestHeader(name = "X-Refresh-Token", required = false) String refreshTokenFromHeader
    ) {
        String token = refreshTokenFromCookie != null ? refreshTokenFromCookie : refreshTokenFromHeader;
        TokenResponse response = authService.refreshToken(token);
        return ResponseEntity.ok(response);
    }

    /**
     * @brief Revokes current session refresh token and clears cookie.
     *
     * @param refreshToken Cookie token value to invalidate.
     * @param response HTTP servlet response to clear cookie.
     * @return HTTP 200 OK.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
        @CookieValue(name = "refreshToken", required = false) String refreshToken,
        HttpServletResponse response
    ) {
        authService.logout(refreshToken);
        clearRefreshTokenCookie(response);
        return ResponseEntity.ok().build();
    }

    /**
     * @brief Invalidate all active refresh tokens for the user across all devices.
     *
     * @param user Authenticated user principal.
     * @param response HTTP servlet response to clear cookie.
     * @return HTTP 200 OK.
     */
    @DeleteMapping("/logout-all")
    public ResponseEntity<Void> logoutAll(
        @AuthenticationPrincipal User user,
        HttpServletResponse response
    ) {
        authService.logoutAll(user);
        clearRefreshTokenCookie(response);
        return ResponseEntity.ok().build();
    }

    /**
     * @brief Initiates password recovery request for the specified account email.
     *
     * @param request Payload containing target account email.
     * @return HTTP 200 OK.
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok().build();
    }

    /**
     * @brief Attaches a strict HttpOnly refresh token cookie to the response using the token issued by AuthService.
     *
     * @param response Target HTTP response.
     * @param tokenResponse TokenResponse containing user metadata and pre-generated refresh token.
     */
    private void setRefreshTokenCookie(HttpServletResponse response, TokenResponse tokenResponse) {
        String refreshToken = tokenResponse.getRefreshToken() != null
            ? tokenResponse.getRefreshToken()
            : jwtService.generateRefreshToken(tokenResponse.getUser().getId(), tokenResponse.getUser().getEmail());
        ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
            .httpOnly(true)
            .secure(false)
            .path("/api/auth")
            .maxAge(30 * 24 * 60 * 60)
            .sameSite("Strict")
            .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /**
     * @brief Clears the client-side refresh token cookie.
     *
     * @param response Target HTTP response.
     */
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
