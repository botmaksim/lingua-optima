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
import lombok.RequiredArgsConstructor;
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

@Slf4j
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final StringRedisTemplate stringRedisTemplate;

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

        // Auto-assign FREE subscription (or EDUCATOR if registered as teacher)
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

    public TokenResponse refreshToken(String refreshToken) {
        if (refreshToken == null || !jwtService.isTokenValid(refreshToken)) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }

        UUID userId = jwtService.extractUserId(refreshToken);
        String email = jwtService.extractEmail(refreshToken);

        // Verify token against Redis store
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

    public void logout(String refreshToken) {
        if (refreshToken != null && stringRedisTemplate != null) {
            String tokenHash = hashToken(refreshToken);
            stringRedisTemplate.delete("refresh_token:" + tokenHash);
        }
    }

    public void logoutAll(User user) {
        if (stringRedisTemplate != null && user != null) {
            String pattern = "refresh_tokens:" + user.getId() + ":*";
            var keys = stringRedisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                stringRedisTemplate.delete(keys);
            }
        }
    }

    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail().toLowerCase().trim())
            .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + request.getEmail()));
        log.info("Password reset request processed for email: {}", user.getEmail());
    }

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

    private void saveRefreshTokenInRedis(UUID userId, String refreshToken) {
        if (stringRedisTemplate == null) return;
        try {
            String tokenHash = hashToken(refreshToken);
            stringRedisTemplate.opsForValue().set("refresh_token:" + tokenHash, userId.toString(), Duration.ofDays(30));
        } catch (Exception e) {
            log.warn("Failed to store refresh token in Redis: {}", e.getMessage());
        }
    }

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
