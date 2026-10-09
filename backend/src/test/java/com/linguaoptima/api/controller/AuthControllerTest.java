/**
 * @file AuthControllerTest.java
 * @brief Unit and slice test suite for AuthController.
 */
package com.linguaoptima.api.controller;

import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.domain.enums.Role;
import com.linguaoptima.api.dto.request.ForgotPasswordRequest;
import com.linguaoptima.api.dto.request.GoogleAuthRequest;
import com.linguaoptima.api.dto.request.LoginRequest;
import com.linguaoptima.api.dto.request.RegisterRequest;
import com.linguaoptima.api.dto.response.TokenResponse;
import com.linguaoptima.api.dto.response.UserResponse;
import com.linguaoptima.api.service.AuthService;
import com.linguaoptima.api.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @brief Unit and slice test suite for AuthController.
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    /** @brief Test fixture or mock dependency for auth service. */
    @Mock
    private AuthService authService;

    /** @brief Test fixture or mock dependency for jwt service. */
    @Mock
    private JwtService jwtService;

    /** @brief Test fixture or mock dependency for auth controller. */
    @InjectMocks
    private AuthController authController;

    /** @brief Test fixture or mock dependency for sample token. */
    private TokenResponse sampleToken;
    /** @brief Test fixture or mock dependency for user. */
    private User user;

    /**
     * @brief Initializes test fixtures and mock state before each test in AuthControllerTest.
     */
    @BeforeEach
    void setUp() {
        user = User.builder().id(UUID.randomUUID()).email("auth@lingua.com").role(Role.STUDENT).build();
        sampleToken = TokenResponse.builder()
            .accessToken("access123")
            .user(UserResponse.builder().id(user.getId()).email("auth@lingua.com").build())
            .build();
    }

    /**
     * @brief Verifies unit test scenario: register.
     */
    @Test
    void testRegister() {
        RegisterRequest req = RegisterRequest.builder().email("auth@lingua.com").password("pass").fullName("Auth").role(Role.STUDENT).build();
        when(authService.register(any())).thenReturn(sampleToken);
        when(jwtService.generateRefreshToken(any(), any())).thenReturn("refresh123");

        MockHttpServletResponse res = new MockHttpServletResponse();
        ResponseEntity<TokenResponse> entity = authController.register(req, res);

        assertEquals(HttpStatus.OK, entity.getStatusCode());
        assertEquals("access123", entity.getBody().getAccessToken());
        assertTrue(res.containsHeader("Set-Cookie"));
    }

    /**
     * @brief Verifies unit test scenario: login.
     */
    @Test
    void testLogin() {
        LoginRequest req = LoginRequest.builder().email("auth@lingua.com").password("pass").build();
        when(authService.login(any())).thenReturn(sampleToken);
        when(jwtService.generateRefreshToken(any(), any())).thenReturn("refresh123");

        MockHttpServletResponse res = new MockHttpServletResponse();
        ResponseEntity<TokenResponse> entity = authController.login(req, res);

        assertEquals(HttpStatus.OK, entity.getStatusCode());
        assertTrue(res.containsHeader("Set-Cookie"));
    }

    /**
     * @brief Verifies unit test scenario: Google OAuth2 login.
     */
    @Test
    void testGoogleLogin() {
        GoogleAuthRequest req = GoogleAuthRequest.builder().idToken("google-id-token").role(Role.STUDENT).build();
        when(authService.googleLogin(any())).thenReturn(sampleToken);
        when(jwtService.generateRefreshToken(any(), any())).thenReturn("refresh123");

        MockHttpServletResponse res = new MockHttpServletResponse();
        ResponseEntity<TokenResponse> entity = authController.googleLogin(req, res);

        assertEquals(HttpStatus.OK, entity.getStatusCode());
        assertEquals("access123", entity.getBody().getAccessToken());
        assertTrue(res.containsHeader("Set-Cookie"));
    }

    /**
     * @brief Verifies unit test scenario: refresh.
     */
    @Test
    void testRefresh() {
        when(authService.refreshToken("cookieToken")).thenReturn(sampleToken);
        ResponseEntity<TokenResponse> entity = authController.refresh("cookieToken", null);
        assertEquals(HttpStatus.OK, entity.getStatusCode());

        TokenResponse withPreGeneratedRefresh = TokenResponse.builder()
            .accessToken("access456")
            .refreshToken("pre-refresh-456")
            .user(sampleToken.getUser())
            .build();
        when(authService.refreshToken("headerToken")).thenReturn(withPreGeneratedRefresh);
        ResponseEntity<TokenResponse> entityFromHeader = authController.refresh(null, "headerToken");
        assertEquals(HttpStatus.OK, entityFromHeader.getStatusCode());

        when(authService.login(any())).thenReturn(withPreGeneratedRefresh);
        MockHttpServletResponse res = new MockHttpServletResponse();
        authController.login(LoginRequest.builder().email("auth@lingua.com").password("pass").build(), res);
        assertTrue(res.containsHeader("Set-Cookie"));
    }


    /**
     * @brief Verifies unit test scenario: logout.
     */
    @Test
    void testLogout() {
        MockHttpServletResponse res = new MockHttpServletResponse();
        ResponseEntity<Void> entity = authController.logout("cookieToken", res);
        assertEquals(HttpStatus.OK, entity.getStatusCode());
        verify(authService).logout("cookieToken");
    }

    /**
     * @brief Verifies unit test scenario: logout all.
     */
    @Test
    void testLogoutAll() {
        MockHttpServletResponse res = new MockHttpServletResponse();
        ResponseEntity<Void> entity = authController.logoutAll(user, res);
        assertEquals(HttpStatus.OK, entity.getStatusCode());
        verify(authService).logoutAll(user);
    }

    /**
     * @brief Verifies unit test scenario: forgot password.
     */
    @Test
    void testForgotPassword() {
        ForgotPasswordRequest req = ForgotPasswordRequest.builder().email("auth@lingua.com").build();
        ResponseEntity<Void> entity = authController.forgotPassword(req);
        assertEquals(HttpStatus.OK, entity.getStatusCode());
        verify(authService).forgotPassword(req);
    }

    /**
     * @brief Verifies that refresh token cookies set Secure=true under direct TLS or Cloudflare Tunnel X-Forwarded-Proto: https.
     */
    @Test
    void testSecureCookieDetectionBehindHttpsAndTunnel() {
        LoginRequest req = LoginRequest.builder().email("auth@lingua.com").password("pass").build();
        when(authService.login(any())).thenReturn(sampleToken);
        when(jwtService.generateRefreshToken(any(), any())).thenReturn("refresh123");

        try {
            // 1. Direct HTTPS request
            MockHttpServletRequest directTlsReq = new MockHttpServletRequest();
            directTlsReq.setSecure(true);
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(directTlsReq));
            MockHttpServletResponse directTlsRes = new MockHttpServletResponse();
            authController.login(req, directTlsRes);
            assertTrue(directTlsRes.getHeader("Set-Cookie").contains("Secure"));

            // 2. Cloudflare Tunnel forwarded HTTPS request (X-Forwarded-Proto: https)
            MockHttpServletRequest tunnelReq = new MockHttpServletRequest();
            tunnelReq.setSecure(false);
            tunnelReq.addHeader("X-Forwarded-Proto", "https");
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(tunnelReq));
            MockHttpServletResponse tunnelRes = new MockHttpServletResponse();
            authController.login(req, tunnelRes);
            assertTrue(tunnelRes.getHeader("Set-Cookie").contains("Secure"));

            // 3. Plain local HTTP request (X-Forwarded-Proto: http)
            MockHttpServletRequest plainHttpReq = new MockHttpServletRequest();
            plainHttpReq.setSecure(false);
            plainHttpReq.addHeader("X-Forwarded-Proto", "http");
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(plainHttpReq));
            MockHttpServletResponse plainHttpRes = new MockHttpServletResponse();
            authController.login(req, plainHttpRes);
            assertFalse(plainHttpRes.getHeader("Set-Cookie").contains("Secure"));
        } finally {
            RequestContextHolder.resetRequestAttributes();
        }
    }
}

