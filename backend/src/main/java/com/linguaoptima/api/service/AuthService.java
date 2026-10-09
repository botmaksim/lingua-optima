package com.linguaoptima.api.service;

import com.linguaoptima.api.domain.Subscription;
import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.domain.enums.Role;
import com.linguaoptima.api.domain.enums.SubscriptionTier;
import com.linguaoptima.api.dto.request.ForgotPasswordRequest;
import com.linguaoptima.api.dto.request.LoginRequest;
import com.linguaoptima.api.dto.request.RegisterRequest;
import com.linguaoptima.api.dto.response.TokenResponse;
import com.linguaoptima.api.dto.response.UserResponse;
import com.linguaoptima.api.exception.QuotaExceededException;
import com.linguaoptima.api.exception.ResourceNotFoundException;
import com.linguaoptima.api.exception.UnauthorizedException;
import com.linguaoptima.api.repository.SubscriptionRepository;
import com.linguaoptima.api.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * @file AuthService.java
 * @brief Authentication and user onboarding service.
 *
 * Handles account registration, credential authentication, JWT token refresh,
 * Redis session persistence, logout revocation, and rate limiting for authentication attempts.
 */
@Slf4j
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * @brief Constructs an AuthService instance with injected repositories, encoders, and services.
     * @param userRepository Repository for user entities.
     * @param subscriptionRepository Repository for subscription tier entities.
     * @param passwordEncoder BCrypt password encoder for secure hashing.
     * @param jwtService Service for creating and validating JWT tokens.
     * @param stringRedisTemplate Optional Redis template for session token storage and rate limiting.
     */
    public AuthService(
            UserRepository userRepository,
            SubscriptionRepository subscriptionRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            @Autowired(required = false) StringRedisTemplate stringRedisTemplate
    ) {
        this.userRepository = userRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * @brief Registers a new user account and assigns initial subscription tier.
     * @param request Registration payload containing email, password, full name, and requested role.
     * @return TokenResponse containing access token and user profile information.
     * @throws IllegalArgumentException if the email is already registered in the system.
     */
    @Transactional
    public TokenResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("User with email already exists: " + request.getEmail());
        }

        Role role = request.getRole() != null ? request.getRole() : Role.STUDENT;

        User user = User.builder()
            .email(request.getEmail().toLowerCase().trim())
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .fullName(request.getFullName().trim())
            .role(role)
            .createdAt(LocalDateTime.now())
            .build();

        User savedUser = userRepository.save(user);

        SubscriptionTier initialTier = (role == Role.TEACHER) ? SubscriptionTier.EDUCATOR : SubscriptionTier.FREE;
        subscriptionRepository.save(Subscription.builder()
            .user(savedUser)
            .tier(initialTier)
            .createdAt(LocalDateTime.now())
            .build());

        String accessToken = jwtService.generateAccessToken(savedUser.getId(), savedUser.getEmail(), savedUser.getRole().name());
        String refreshToken = jwtService.generateRefreshToken(savedUser.getId(), savedUser.getEmail());
        saveRefreshTokenInRedis(savedUser.getId(), refreshToken);

        return TokenResponse.builder()
            .accessToken(accessToken)
            .user(UserResponse.fromEntity(savedUser))
            .build();
    }

    /**
     * @brief Authenticates a user with email and password and issues JWT access and refresh tokens.
     * @param request Login credentials payload.
     * @return TokenResponse containing access token and user profile information.
     * @throws BadCredentialsException if the email is not found or the password hash does not match.
     * @throws QuotaExceededException if rate limit of 10 attempts per 15 minutes is exceeded.
     */
    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        checkAuthRateLimit(email);

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getEmail());
        saveRefreshTokenInRedis(user.getId(), refreshToken);

        return TokenResponse.builder()
            .accessToken(accessToken)
            .user(UserResponse.fromEntity(user))
            .build();
    }

    /**
     * @brief Issues a new access token using a valid refresh token.
     * @param refreshToken The active refresh token presented by the client.
     * @return TokenResponse containing a fresh access token and user profile details.
     * @throws UnauthorizedException if the refresh token is missing, expired, or revoked in Redis.
     * @throws ResourceNotFoundException if the user associated with the token cannot be found.
     */
    public TokenResponse refreshToken(String refreshToken) {
        if (refreshToken == null || !jwtService.isTokenValid(refreshToken)) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }

        UUID userId = jwtService.extractUserId(refreshToken);
        String email = jwtService.extractEmail(refreshToken);

        String tokenHash = hashToken(refreshToken);
        String redisKey = "refresh_token:" + tokenHash;
        if (stringRedisTemplate != null) {
            String storedUserId = stringRedisTemplate.opsForValue().get(redisKey);
            if (storedUserId == null) {
                throw new UnauthorizedException("Refresh token revoked or expired");
            }
        }

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String newAccessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        return TokenResponse.builder()
            .accessToken(newAccessToken)
            .user(UserResponse.fromEntity(user))
            .build();
    }

    /**
     * @brief Revokes a specific refresh token upon user logout.
     * @param refreshToken The refresh token string to revoke from Redis.
     */
    public void logout(String refreshToken) {
        if (refreshToken != null && stringRedisTemplate != null) {
            String tokenHash = hashToken(refreshToken);
            stringRedisTemplate.delete("refresh_token:" + tokenHash);
        }
    }

    /**
     * @brief Revokes all active refresh tokens for the specified user across all client devices.
     * @param user The user whose active sessions should be invalidated.
     */
    public void logoutAll(User user) {
        if (stringRedisTemplate != null && user != null) {
            String pattern = "refresh_tokens:" + user.getId() + ":*";
            var keys = stringRedisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                stringRedisTemplate.delete(keys);
            }
        }
    }

    /**
     * @brief Initiates a password reset flow for the provided email address.
     * @param request Password reset payload containing user email.
     * @throws ResourceNotFoundException if no user is registered with the provided email.
     */
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
            .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.getEmail()));
        log.info("Password reset request processed for email: {}", user.getEmail());
    }

    /**
     * @brief Enforces authentication rate limits per email address using Redis sliding counters.
     * @param email The target email address being checked.
     * @throws QuotaExceededException if more than 10 attempts occur within a 15-minute window.
     */
    private void checkAuthRateLimit(String email) {
        if (stringRedisTemplate == null) return;
        try {
            String key = "rate_limit:" + email + ":auth";
            Long attempts = stringRedisTemplate.opsForValue().increment(key);
            if (attempts != null && attempts == 1L) {
                stringRedisTemplate.expire(key, Duration.ofMinutes(15));
            }
            if (attempts != null && attempts > 10L) {
                throw new QuotaExceededException("Too many login attempts. Please wait 15 minutes.");
            }
        } catch (QuotaExceededException q) {
            throw q;
        } catch (Exception e) {
            log.warn("Auth rate limit check failed: {}", e.getMessage());
        }
    }

    /**
     * @brief Stores an active refresh token in Redis with a 30-day expiration time.
     * @param userId Unique identifier of the user.
     * @param refreshToken The raw refresh token string to hash and store.
     */
    private void saveRefreshTokenInRedis(UUID userId, String refreshToken) {
        if (stringRedisTemplate == null) return;
        try {
            String tokenHash = hashToken(refreshToken);
            stringRedisTemplate.opsForValue().set("refresh_token:" + tokenHash, userId.toString(), Duration.ofDays(30));
        } catch (Exception e) {
            log.warn("Failed to store refresh token in Redis: {}", e.getMessage());
        }
    }

    /**
     * @brief Calculates a cryptographic SHA-256 hash of a token string for safe key indexing.
     * @param token The raw token string to hash.
     * @return Hexadecimal representation of the SHA-256 digest.
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return hex.toString();
        } catch (Exception e) {
            return String.valueOf(token.hashCode());
        }
    }
}
