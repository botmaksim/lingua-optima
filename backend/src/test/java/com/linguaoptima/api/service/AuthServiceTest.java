/**
 * @file AuthServiceTest.java
 * @brief Unit and slice test suite for AuthService.
 */
package com.linguaoptima.api.service;

import com.linguaoptima.api.domain.Subscription;
import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.domain.enums.Role;
import com.linguaoptima.api.domain.enums.SubscriptionTier;
import com.linguaoptima.api.dto.request.ForgotPasswordRequest;
import com.linguaoptima.api.dto.request.GoogleAuthRequest;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * @brief Unit and slice test suite for AuthService.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    /** @brief Test fixture or mock dependency for user repository. */
    @Mock
    private UserRepository userRepository;

    /** @brief Test fixture or mock dependency for subscription repository. */
    @Mock
    private SubscriptionRepository subscriptionRepository;

    /** @brief Test fixture or mock dependency for password encoder. */
    @Mock
    private PasswordEncoder passwordEncoder;

    /** @brief Test fixture or mock dependency for jwt service. */
    @Mock
    private JwtService jwtService;

    /** @brief Test fixture or mock dependency for string redis template. */
    @Mock
    private StringRedisTemplate stringRedisTemplate;

    /** @brief Test fixture or mock dependency for value operations. */
    @Mock
    private ValueOperations<String, String> valueOperations;

    /** @brief Test fixture or mock dependency for auth service. */
    private AuthService authService;

    /** @brief Test fixture or mock dependency for sample user. */
    private User sampleUser;

    /**
     * @brief Initializes test fixtures and mock state before each test in AuthServiceTest.
     */
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

    /**
     * @brief Verifies unit test scenario: register student success.
     */
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

    /**
     * @brief Verifies unit test scenario: register teacher success.
     */
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

    /**
     * @brief Verifies unit test scenario: register duplicate email throws.
     */
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

    /**
     * @brief Verifies unit test scenario: login success.
     */
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

    /**
     * @brief Verifies unit test scenario: login bad password throws.
     */
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

    /**
     * @brief Verifies unit test scenario: login user not found throws.
     */
    @Test
    void testLoginUserNotFoundThrows() {
        LoginRequest req = LoginRequest.builder()
            .email("unknown@lingua.com")
            .password("wrong")
            .build();

        when(userRepository.findByEmail("unknown@lingua.com")).thenReturn(Optional.empty());
        assertThrows(BadCredentialsException.class, () -> authService.login(req));
    }

    /**
     * @brief Verifies unit test scenario: login rate limit exceeded.
     */
    @Test
    void testLoginRateLimitExceeded() {
        LoginRequest req = LoginRequest.builder().email("rate@lingua.com").password("pwd").build();
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment("rate_limit:rate@lingua.com:auth")).thenReturn(15L);

        assertThrows(QuotaExceededException.class, () -> authService.login(req));
    }

    /**
     * @brief Verifies unit test scenario: Google OAuth2 login for existing user and new student/teacher accounts.
     */
    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void testGoogleLoginExistingAndNewUsers() {
        RestClient mockClient = mock(RestClient.class);
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(mockClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString(), any(Object[].class))).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);

        authService.setGoogleRestClient(mockClient);
        authService.setGoogleClientId("my-google-client-id");

        when(responseSpec.body(Map.class)).thenReturn(Map.of(
            "email", "student@lingua.com",
            "email_verified", "true",
            "aud", "my-google-client-id",
            "name", "John Doe"
        ));
        when(userRepository.findByEmail("student@lingua.com")).thenReturn(Optional.of(sampleUser));
        when(jwtService.generateAccessToken(any(), any(), any())).thenReturn("google-access");
        when(jwtService.generateRefreshToken(any(), any())).thenReturn("google-refresh");

        TokenResponse existingRes = authService.googleLogin(GoogleAuthRequest.builder().idToken("valid-id-token").build());
        assertEquals("google-access", existingRes.getAccessToken());

        when(responseSpec.body(Map.class)).thenReturn(Map.of(
            "email", "newstudent@lingua.com",
            "email_verified", "true",
            "aud", "my-google-client-id",
            "name", "New Student"
        ));
        when(userRepository.findByEmail("newstudent@lingua.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("randomHash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        TokenResponse newStudentRes = authService.googleLogin(GoogleAuthRequest.builder().idToken("valid-id-token-2").build());
        assertEquals("newstudent@lingua.com", newStudentRes.getUser().getEmail());

        when(responseSpec.body(Map.class)).thenReturn(Map.of(
            "email", "newteacher@lingua.com",
            "email_verified", "true",
            "aud", "my-google-client-id"
        ));
        when(userRepository.findByEmail("newteacher@lingua.com")).thenReturn(Optional.empty());

        TokenResponse newTeacherRes = authService.googleLogin(
            GoogleAuthRequest.builder().idToken("valid-id-token-3").role(Role.TEACHER).build()
        );
        assertEquals(Role.TEACHER, newTeacherRes.getUser().getRole());
    }

    /**
     * @brief Verifies unit test scenario: Google OAuth2 invalid token, unverified email, and audience mismatch errors.
     */
    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void testGoogleLoginValidationErrors() {
        assertThrows(UnauthorizedException.class, () -> authService.googleLogin(null));
        assertThrows(UnauthorizedException.class, () -> authService.googleLogin(GoogleAuthRequest.builder().idToken(" ").build()));

        RestClient mockClient = mock(RestClient.class);
        RestClient.RequestHeadersUriSpec uriSpec = mock(RestClient.RequestHeadersUriSpec.class);
        RestClient.RequestHeadersSpec headersSpec = mock(RestClient.RequestHeadersSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(mockClient.get()).thenReturn(uriSpec);
        when(uriSpec.uri(anyString(), any(Object[].class))).thenReturn(headersSpec);
        when(headersSpec.retrieve()).thenReturn(responseSpec);
        authService.setGoogleRestClient(mockClient);

        when(responseSpec.body(Map.class)).thenThrow(new RuntimeException("HTTP 400"));
        assertThrows(UnauthorizedException.class, () ->
            authService.googleLogin(GoogleAuthRequest.builder().idToken("bad-token").build())
        );

        reset(responseSpec);
        when(responseSpec.body(Map.class)).thenReturn(Map.of("email", ""));
        assertThrows(UnauthorizedException.class, () ->
            authService.googleLogin(GoogleAuthRequest.builder().idToken("no-email-token").build())
        );

        when(responseSpec.body(Map.class)).thenReturn(Map.of("email", "a@b.com", "email_verified", "false"));
        assertThrows(UnauthorizedException.class, () ->
            authService.googleLogin(GoogleAuthRequest.builder().idToken("unverified-token").build())
        );

        authService.setGoogleClientId("expected-client-id");
        when(responseSpec.body(Map.class)).thenReturn(Map.of("email", "a@b.com", "email_verified", "true", "aud", "wrong-aud"));
        assertThrows(UnauthorizedException.class, () ->
            authService.googleLogin(GoogleAuthRequest.builder().idToken("wrong-aud-token").build())
        );
    }

    /**
     * @brief Verifies unit test scenario: refresh token success.
     */
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

    /**
     * @brief Verifies unit test scenario: refresh token invalid throws.
     */
    @Test
    void testRefreshTokenInvalidThrows() {
        when(jwtService.isTokenValid("bad-token")).thenReturn(false);
        assertThrows(UnauthorizedException.class, () -> authService.refreshToken("bad-token"));
    }

    /**
     * @brief Verifies unit test scenario: refresh token revoked in redis throws.
     */
    @Test
    void testRefreshTokenRevokedInRedisThrows() {
        String ref = "revoked-token";
        when(jwtService.isTokenValid(ref)).thenReturn(true);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        assertThrows(UnauthorizedException.class, () -> authService.refreshToken(ref));
    }

    /**
     * @brief Verifies unit test scenario: logout.
     */
    @Test
    void testLogout() {
        authService.logout("refresh-to-delete");
        verify(stringRedisTemplate).delete(anyString());
    }

    /**
     * @brief Verifies unit test scenario: logout all.
     */
    @Test
    void testLogoutAll() {
        String prefix = "refresh_tokens:" + sampleUser.getId() + ":";
        when(stringRedisTemplate.keys(prefix + "*"))
            .thenReturn(Set.of(prefix + "hash1", prefix + "hash2"));
        when(valueOperations.increment(anyString())).thenReturn(1L);

        authService.logoutAll(sampleUser);
        verify(stringRedisTemplate).delete(any(Set.class));
    }

    /**
     * @brief Verifies unit test scenario: forgot password.
     */
    @Test
    void testForgotPassword() {
        when(userRepository.findByEmail("student@lingua.com")).thenReturn(Optional.of(sampleUser));
        assertDoesNotThrow(() -> authService.forgotPassword(ForgotPasswordRequest.builder().email("student@lingua.com").build()));

        when(userRepository.findByEmail("none@lingua.com")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> authService.forgotPassword(ForgotPasswordRequest.builder().email("none@lingua.com").build()));
    }

    /**
     * @brief Verifies unit test scenario: Redis null, rate-limit first attempt, Redis exceptions, and missing user on refresh.
     */
    @Test
    void testRedisNullAndErrorBranches() {
        AuthService noRedisAuth = new AuthService(userRepository, subscriptionRepository, passwordEncoder, jwtService, null);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPass");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(jwtService.generateAccessToken(any(), any(), any())).thenReturn("acc");
        when(jwtService.generateRefreshToken(any(), any())).thenReturn("ref");

        TokenResponse regNoRole = noRedisAuth.register(
            RegisterRequest.builder().email("norole@lingua.com").password("pass123").fullName("No Role").role(null).build()
        );
        assertNotNull(regNoRole);

        when(userRepository.findByEmail("student@lingua.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.matches("password123", "encodedPass")).thenReturn(true);
        assertNotNull(noRedisAuth.login(LoginRequest.builder().email("student@lingua.com").password("password123").build()));

        when(valueOperations.increment("rate_limit:student@lingua.com:auth")).thenReturn(1L);
        doThrow(new RuntimeException("Redis write fail")).when(valueOperations).set(anyString(), anyString(), any());
        assertNotNull(authService.login(LoginRequest.builder().email("student@lingua.com").password("password123").build()));

        when(valueOperations.increment("rate_limit:student@lingua.com:auth")).thenThrow(new RuntimeException("Redis down"));
        assertNotNull(authService.login(LoginRequest.builder().email("student@lingua.com").password("password123").build()));

        String ref = "valid-ref-missing-user";
        UUID missingUid = UUID.randomUUID();
        when(jwtService.isTokenValid(ref)).thenReturn(true);
        when(jwtService.extractUserId(ref)).thenReturn(missingUid);
        when(valueOperations.get(anyString())).thenReturn(missingUid.toString());
        when(userRepository.findById(missingUid)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> authService.refreshToken(ref));
    }
}


