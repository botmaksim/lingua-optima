/**
 * @file AuthService.java
 * @brief Authentication and user onboarding service.
 */
package com.linguaoptima.api.service;

import com.linguaoptima.api.domain.Subscription;
import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.domain.enums.CefrLevel;
import com.linguaoptima.api.domain.enums.Role;
import com.linguaoptima.api.domain.enums.SubscriptionTier;
import com.linguaoptima.api.dto.request.ForgotPasswordRequest;
import com.linguaoptima.api.dto.request.GoogleAuthRequest;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @brief Authentication and user onboarding service.
 *
 * Handles account registration, email/password authentication, Google OAuth2 authentication,
 * JWT token refresh, Redis session persistence, logout revocation, and rate limiting.
 */
@Slf4j
@Service
public class AuthService {

    /** @brief Field representing user repository in AuthService. */
    private final UserRepository userRepository;
    /** @brief Field representing subscription repository in AuthService. */
    private final SubscriptionRepository subscriptionRepository;
    /** @brief Field representing password encoder in AuthService. */
    private final PasswordEncoder passwordEncoder;
    /** @brief Field representing jwt service in AuthService. */
    private final JwtService jwtService;
    /** @brief Field representing string redis template in AuthService. */
    private final StringRedisTemplate stringRedisTemplate;
    /** @brief Field representing email service in AuthService. */
    private final EmailService emailService;

    /** @brief Cryptographically secure random generator for 6-digit confirmation codes. */
    private final SecureRandom secureRandom = new SecureRandom();
    /** @brief In-memory fallback storage for email verification codes when Redis is unavailable. */
    private final Map<String, VerificationEntry> fallbackVerificationCodes = new ConcurrentHashMap<>();

    /**
     * @brief Internal record storing verification code and expiration timestamp.
     * @param code 6-digit confirmation code.
     * @param expiresAt Expiration timestamp.
     */
    private record VerificationEntry(String code, LocalDateTime expiresAt) {}

    /** @brief Configured Google OAuth2 Client ID for verifying token audience. */
    @Value("${app.oauth.google.client-id:}")
    private String googleClientId = "";

    /** @brief HTTP client for verifying Google OAuth2 ID tokens against Google's tokeninfo endpoint. */
    private RestClient googleRestClient = RestClient.builder()
        .baseUrl("https://oauth2.googleapis.com")
        .build();

    /**
     * @brief Constructs an AuthService instance with injected repositories, encoders, and services.
     * @param userRepository Repository for user entities.
     * @param subscriptionRepository Repository for subscription tier entities.
     * @param passwordEncoder BCrypt password encoder for secure hashing.
     * @param jwtService Service for creating and validating JWT tokens.
     * @param stringRedisTemplate Optional Redis template for session token storage and rate limiting.
     * @param emailService Service for dispatching verification emails.
     */
    public AuthService(
            UserRepository userRepository,
            SubscriptionRepository subscriptionRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            @Autowired(required = false) StringRedisTemplate stringRedisTemplate,
            EmailService emailService
    ) {
        this.userRepository = userRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.stringRedisTemplate = stringRedisTemplate;
        this.emailService = emailService;
    }

    /**
     * @brief Sets the Google OAuth2 Client ID used for audience verification.
     * @param googleClientId Configured Google OAuth2 Client ID.
     */
    public void setGoogleClientId(String googleClientId) {
        this.googleClientId = googleClientId;
    }

    /**
     * @brief Overrides the RestClient used for Google OAuth2 token verification.
     * @param googleRestClient Custom or mock RestClient instance.
     */
    public void setGoogleRestClient(RestClient googleRestClient) {
        this.googleRestClient = googleRestClient;
    }

    /**
     * @brief Registers a new user account verifying the email confirmation code and assigns initial subscription tier.
     * @param request Registration payload containing email, password, full name, requested role, and 6-digit confirmation code.
     * @return TokenResponse containing access token and user profile information.
     * @throws IllegalArgumentException if the email is already registered or verification code is missing.
     * @throws BadCredentialsException if the confirmation code is invalid or expired.
     */
    @Transactional
    public TokenResponse register(RegisterRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("User with email already exists: " + email);
        }

        String code = request.getVerificationCode() != null ? request.getVerificationCode().trim() : "";
        if (code.isBlank()) {
            throw new IllegalArgumentException("Verification code is required for email registration");
        }
        if (!verifyAndConsumeCode(email, code)) {
            throw new BadCredentialsException("Invalid or expired verification code");
        }

        Role role = request.getRole() != null ? request.getRole() : Role.STUDENT;
        CefrLevel cefrLevel = request.getCefrLevel() != null ? request.getCefrLevel() : CefrLevel.A1;

        String chosenName = request.getFullName().trim();
        User user = User.builder()
            .email(email)
            .passwordHash(passwordEncoder.encode(request.getPassword()))
            .fullName(chosenName)
            .displayAlias(chosenName)
            .role(role)
            .cefrLevel(cefrLevel)
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
            .refreshToken(refreshToken)
            .user(UserResponse.fromEntity(savedUser))
            .build();
    }

    /**
     * @brief Dispatches a 6-digit email confirmation code for new registration accounts.
     * @param email Recipient email address.
     * @throws IllegalArgumentException if email is missing or already taken.
     * @throws QuotaExceededException if code requests are throttled (more than 1 per 60 seconds).
     */
    public void sendRegistrationVerificationCode(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Valid email address is required");
        }
        String normalizedEmail = email.toLowerCase().trim();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("User with email already exists: " + normalizedEmail);
        }

        checkVerificationCodeRateLimit(normalizedEmail);

        int randomPin = secureRandom.nextInt(900_000) + 100_000;
        String code = String.valueOf(randomPin);

        saveVerificationCode(normalizedEmail, code);
        emailService.sendVerificationCode(normalizedEmail, code);
        log.info("Registration verification code sent to {}", normalizedEmail);
    }

    /**
     * @brief Enforces a 60-second cooldown per email for verification code requests.
     * @param email Target email address.
     * @throws QuotaExceededException if a code was already requested within 60 seconds.
     */
    private void checkVerificationCodeRateLimit(String email) {
        if (stringRedisTemplate != null) {
            try {
                String key = "rate_limit:" + email + ":verification_send";
                Boolean wasSet = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofSeconds(60));
                if (Boolean.FALSE.equals(wasSet)) {
                    throw new QuotaExceededException("A verification code was recently sent. Please wait 60 seconds before requesting a new code.");
                }
            } catch (QuotaExceededException q) {
                throw q;
            } catch (Exception e) {
                log.warn("Redis verification rate limit check skipped: {}", e.getMessage());
            }
        }
    }

    /**
     * @brief Persists an active verification code in Redis with a 10-minute TTL and in-memory fallback.
     * @param email Recipient email.
     * @param code 6-digit confirmation code.
     */
    private void saveVerificationCode(String email, String code) {
        if (stringRedisTemplate != null) {
            try {
                stringRedisTemplate.opsForValue().set("verification_code:" + email, code, Duration.ofMinutes(10));
            } catch (Exception e) {
                log.warn("Failed to persist verification code in Redis: {}", e.getMessage());
            }
        }
        fallbackVerificationCodes.put(email, new VerificationEntry(code, LocalDateTime.now().plusMinutes(10)));
    }

    /**
     * @brief Verifies and consumes a 6-digit verification code.
     * @param email Recipient email.
     * @param code 6-digit code presented by user.
     * @return true if valid and consumed, false otherwise.
     */
    private boolean verifyAndConsumeCode(String email, String code) {
        String cleanCode = code.trim();

        if (stringRedisTemplate != null) {
            try {
                String key = "verification_code:" + email;
                String stored = stringRedisTemplate.opsForValue().get(key);
                if (cleanCode.equals(stored)) {
                    stringRedisTemplate.delete(key);
                    fallbackVerificationCodes.remove(email);
                    return true;
                }
            } catch (Exception e) {
                log.warn("Failed to read verification code from Redis: {}", e.getMessage());
            }
        }

        VerificationEntry entry = fallbackVerificationCodes.get(email);
        if (entry != null && LocalDateTime.now().isBefore(entry.expiresAt()) && cleanCode.equals(entry.code())) {
            fallbackVerificationCodes.remove(email);
            return true;
        }

        return false;
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
            .refreshToken(refreshToken)
            .user(UserResponse.fromEntity(user))
            .build();
    }

    /**
     * @brief Authenticates or auto-provisions a user via a verified Google OAuth2 ID token.
     * @param request Google OAuth2 payload containing the ID token and optional role for new accounts.
     * @return TokenResponse containing access token, refresh token, and user profile details.
     * @throws UnauthorizedException if the Google ID token is missing, invalid, unverified, or has an audience mismatch.
     */
    @Transactional
    public TokenResponse googleLogin(GoogleAuthRequest request) {
        if (request == null || request.getIdToken() == null || request.getIdToken().isBlank()) {
            throw new UnauthorizedException("Google ID token is required");
        }

        Map<String, Object> tokenInfo = verifyGoogleIdToken(request.getIdToken().trim());
        String email = String.valueOf(tokenInfo.get("email")).toLowerCase().trim();
        checkAuthRateLimit(email);

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            Role role = request.getRole() != null ? request.getRole() : Role.STUDENT;
            String rawName = tokenInfo.get("name") != null ? String.valueOf(tokenInfo.get("name")).trim() : "";
            String fullName = !rawName.isBlank() ? rawName : email.split("@")[0];

            User newUser = userRepository.save(User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                .fullName(fullName)
                .role(role)
                .createdAt(LocalDateTime.now())
                .build());

            SubscriptionTier initialTier = (role == Role.TEACHER) ? SubscriptionTier.EDUCATOR : SubscriptionTier.FREE;
            subscriptionRepository.save(Subscription.builder()
                .user(newUser)
                .tier(initialTier)
                .createdAt(LocalDateTime.now())
                .build());
            return newUser;
        });

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole().name());
        String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getEmail());
        saveRefreshTokenInRedis(user.getId(), refreshToken);

        return TokenResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .user(UserResponse.fromEntity(user))
            .build();
    }

    /**
     * @brief Verifies a Google OAuth2 ID token against Google's tokeninfo endpoint and checks audience and email verification claims.
     * @param idToken Raw Google ID token string.
     * @return Parsed map of claims returned by Google tokeninfo.
     * @throws UnauthorizedException if token verification fails, email is unverified, or audience does not match GOOGLE_CLIENT_ID.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> verifyGoogleIdToken(String idToken) {
        Map<String, Object> tokenInfo;
        try {
            tokenInfo = googleRestClient.get()
                .uri("/tokeninfo?id_token={token}", idToken)
                .retrieve()
                .body(Map.class);
        } catch (Exception e) {
            throw new UnauthorizedException("Invalid or expired Google OAuth token");
        }

        if (tokenInfo == null || tokenInfo.get("email") == null || String.valueOf(tokenInfo.get("email")).isBlank()) {
            throw new UnauthorizedException("Google token does not contain a valid email");
        }

        Object emailVerified = tokenInfo.get("email_verified");
        if (emailVerified != null && !"true".equalsIgnoreCase(String.valueOf(emailVerified))) {
            throw new UnauthorizedException("Google account email is not verified");
        }

        if (googleClientId != null && !googleClientId.isBlank()) {
            String aud = tokenInfo.get("aud") != null ? String.valueOf(tokenInfo.get("aud")) : "";
            if (!googleClientId.trim().equals(aud)) {
                throw new UnauthorizedException("Google token audience mismatch");
            }
        }

        return tokenInfo;
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
            String prefix = "refresh_tokens:" + user.getId() + ":";
            String pattern = prefix + "*";
            var keys = stringRedisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                java.util.Set<String> allKeysToDelete = new java.util.HashSet<>(keys);
                for (String key : keys) {
                    if (key.startsWith(prefix)) {
                        String hash = key.substring(prefix.length());
                        allKeysToDelete.add("refresh_token:" + hash);
                    }
                }
                stringRedisTemplate.delete(allKeysToDelete);
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
            stringRedisTemplate.opsForValue().set("refresh_tokens:" + userId + ":" + tokenHash, tokenHash, Duration.ofDays(30));
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
            return String.valueOf(java.util.Objects.hashCode(token));
        }
    }
}
