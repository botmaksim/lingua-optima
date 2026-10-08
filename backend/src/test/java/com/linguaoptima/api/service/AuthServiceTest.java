package com.linguaoptima.api.service;

import com.linguaoptima.api.domain.Subscription;
import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.domain.enums.Role;
import com.linguaoptima.api.domain.enums.SubscriptionTier;
import com.linguaoptima.api.dto.request.ForgotPasswordRequest;
import com.linguaoptima.api.dto.request.LoginRequest;
import com.linguaoptima.api.dto.request.RegisterRequest;
import com.linguaoptima.api.dto.response.TokenResponse;
import com.linguaoptima.api.exception.QuotaExceededException;
import com.linguaoptima.api.exception.ResourceNotFoundException;
import com.linguaoptima.api.exception.UnauthorizedException;
import com.linguaoptima.api.repository.SubscriptionRepository;
import com.linguaoptima.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.Set;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private AuthService authService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        authService = new AuthService(userRepository, subscriptionRepository, passwordEncoder, jwtService, stringRedisTemplate);

        sampleUser = User.builder()
            .id(UUID.randomUUID())
            .email("student@lingua.com")
            .fullName("John Doe")
            .passwordHash("encodedPass")
            .role(Role.STUDENT)
            .build();
    }

    @Test
    void testRegisterStudentSuccess() {
        RegisterRequest req = RegisterRequest.builder()
            .email("student@lingua.com")
            .password("password123")
            .fullName("John Doe")
            .role(Role.STUDENT)
            .build();

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPass");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(jwtService.generateAccessToken(any(), any(), any())).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any(), any())).thenReturn("refresh-token");

        TokenResponse response = authService.register(req);

        assertNotNull(response);
        assertEquals("access-token", response.getAccessToken());
        assertEquals("student@lingua.com", response.getUser().getEmail());
        verify(subscriptionRepository).save(argThat(sub -> sub.getTier() == SubscriptionTier.FREE));
    }

    @Test
    void testRegisterTeacherSuccess() {
        RegisterRequest req = RegisterRequest.builder()
            .email("teacher@lingua.com")
            .password("password123")
            .fullName("Jane Teacher")
            .role(Role.TEACHER)
            .build();

        User teacher = User.builder()
            .id(UUID.randomUUID())
            .email("teacher@lingua.com")
            .fullName("Jane Teacher")
            .role(Role.TEACHER)
            .build();

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPass");
        when(userRepository.save(any(User.class))).thenReturn(teacher);
        when(jwtService.generateAccessToken(any(), any(), any())).thenReturn("teacher-access");
        when(jwtService.generateRefreshToken(any(), any())).thenReturn("teacher-refresh");

        TokenResponse response = authService.register(req);

        assertNotNull(response);
        verify(subscriptionRepository).save(argThat(sub -> sub.getTier() == SubscriptionTier.EDUCATOR));
    }

    @Test
    void testRegisterDuplicateEmailThrows() {
        RegisterRequest req = RegisterRequest.builder()
            .email("existing@lingua.com")
            .password("pass")
            .fullName("Name")
            .role(Role.STUDENT)
            .build();

        when(userRepository.existsByEmail("existing@lingua.com")).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> authService.register(req));
    }

    @Test
    void testLoginSuccess() {
        LoginRequest req = LoginRequest.builder()
            .email("student@lingua.com")
            .password("password123")
            .build();

        when(userRepository.findByEmail("student@lingua.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("password123", "encodedPass")).thenReturn(true);
        when(jwtService.generateAccessToken(any(), any(), any())).thenReturn("token123");
        when(jwtService.generateRefreshToken(any(), any())).thenReturn("ref123");

        TokenResponse res = authService.login(req);
        assertNotNull(res);
        assertEquals("token123", res.getAccessToken());
    }

    @Test
    void testLoginBadPasswordThrows() {
        LoginRequest req = LoginRequest.builder()
            .email("student@lingua.com")
            .password("wrong")
            .build();

        when(userRepository.findByEmail("student@lingua.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("wrong", "encodedPass")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> authService.login(req));
    }

    @Test
    void testLoginUserNotFoundThrows() {
        LoginRequest req = LoginRequest.builder()
            .email("unknown@lingua.com")
            .password("wrong")
            .build();

        when(userRepository.findByEmail("unknown@lingua.com")).thenReturn(Optional.empty());
        assertThrows(BadCredentialsException.class, () -> authService.login(req));
    }

    @Test
    void testLoginRateLimitExceeded() {
        LoginRequest req = LoginRequest.builder().email("rate@lingua.com").password("pwd").build();
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("rate_limit:rate@lingua.com:auth")).thenReturn(15L);

        assertThrows(QuotaExceededException.class, () -> authService.login(req));
    }

    @Test
    void testRefreshTokenSuccess() {
        String ref = "valid-refresh";
        UUID uid = sampleUser.getId();

        when(jwtService.isTokenValid(ref)).thenReturn(true);
        when(jwtService.extractUserId(ref)).thenReturn(uid);
        when(jwtService.extractEmail(ref)).thenReturn(sampleUser.getEmail());
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(uid.toString());
        when(userRepository.findById(uid)).thenReturn(Optional.of(sampleUser));
        when(jwtService.generateAccessToken(any(), any(), any())).thenReturn("new-access");

        TokenResponse tokenResponse = authService.refreshToken(ref);
        assertNotNull(tokenResponse);
        assertEquals("new-access", tokenResponse.getAccessToken());
    }

    @Test
    void testRefreshTokenInvalidThrows() {
        when(jwtService.isTokenValid("bad-token")).thenReturn(false);
        assertThrows(UnauthorizedException.class, () -> authService.refreshToken("bad-token"));
    }

    @Test
    void testRefreshTokenRevokedInRedisThrows() {
        String ref = "revoked-token";
        when(jwtService.isTokenValid(ref)).thenReturn(true);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        assertThrows(UnauthorizedException.class, () -> authService.refreshToken(ref));
    }

    @Test
    void testLogout() {
        authService.logout("refresh-to-delete");
        verify(stringRedisTemplate).delete(anyString());
    }

    @Test
    void testLogoutAll() {
        when(stringRedisTemplate.keys("refresh_tokens:" + sampleUser.getId() + ":*"))
            .thenReturn(Set.of("key1", "key2"));

        authService.logoutAll(sampleUser);
        verify(stringRedisTemplate).delete(any(Set.class));
    }

    @Test
    void testForgotPassword() {
        when(userRepository.findByEmail("student@lingua.com")).thenReturn(Optional.of(sampleUser));
        assertDoesNotThrow(() -> authService.forgotPassword(ForgotPasswordRequest.builder().email("student@lingua.com").build()));

        when(userRepository.findByEmail("none@lingua.com")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> authService.forgotPassword(ForgotPasswordRequest.builder().email("none@lingua.com").build()));
    }
}
