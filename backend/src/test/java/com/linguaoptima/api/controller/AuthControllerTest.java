package com.linguaoptima.api.controller;

import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.domain.enums.Role;
import com.linguaoptima.api.dto.request.ForgotPasswordRequest;
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
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @file AuthControllerTest.java
 * @brief Unit and slice test suite for AuthController.
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthController authController;

    private TokenResponse sampleToken;
    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().id(UUID.randomUUID()).email("auth@lingua.com").role(Role.STUDENT).build();
        sampleToken = TokenResponse.builder()
            .accessToken("access123")
            .user(UserResponse.builder().id(user.getId()).email("auth@lingua.com").build())
            .build();
    }

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

    @Test
    void testRefresh() {
        when(authService.refreshToken("cookieToken")).thenReturn(sampleToken);
        ResponseEntity<TokenResponse> entity = authController.refresh("cookieToken", null);
        assertEquals(HttpStatus.OK, entity.getStatusCode());
    }

    @Test
    void testLogout() {
        MockHttpServletResponse res = new MockHttpServletResponse();
        ResponseEntity<Void> entity = authController.logout("cookieToken", res);
        assertEquals(HttpStatus.OK, entity.getStatusCode());
        verify(authService).logout("cookieToken");
    }

    @Test
    void testLogoutAll() {
        MockHttpServletResponse res = new MockHttpServletResponse();
        ResponseEntity<Void> entity = authController.logoutAll(user, res);
        assertEquals(HttpStatus.OK, entity.getStatusCode());
        verify(authService).logoutAll(user);
    }

    @Test
    void testForgotPassword() {
        ForgotPasswordRequest req = ForgotPasswordRequest.builder().email("auth@lingua.com").build();
        ResponseEntity<Void> entity = authController.forgotPassword(req);
        assertEquals(HttpStatus.OK, entity.getStatusCode());
        verify(authService).forgotPassword(req);
    }
}
