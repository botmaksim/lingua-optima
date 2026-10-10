/**
 * @file GenerationProtectionService.java
 * @brief Enforces single-device binding and single concurrent AI generation locks to prevent multi-device sharing and API abuse.
 */
package com.linguaoptima.api.service;

import com.linguaoptima.api.exception.ForbiddenException;
import com.linguaoptima.api.exception.QuotaExceededException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @brief Manages active device bindings and in-flight generation concurrency per user.
 *
 * Guarantees that:
 * 1. A user account can only initiate AI task generations from a single bound device at a time.
 * 2. Only one AI generation request can run concurrently per user account (blocking parallel spam/abuse).
 */
@Service
@Slf4j
public class GenerationProtectionService {

    /** @brief Safety lease timeout after which an in-flight generation lock automatically expires. */
    public static final Duration LEASE_TIMEOUT = Duration.ofSeconds(90);

    /** @brief Inactivity period after which a bound device session expires and a new device may be bound. */
    public static final Duration DEVICE_SESSION_TTL = Duration.ofHours(12);

    /** @brief Record capturing an active generation lock lease. */
    public record ActiveGeneration(UUID userId, String deviceId, Instant acquiredAt) {}

    /** @brief Record capturing a user's bound device session. */
    public record DeviceBinding(String deviceId, Instant boundAt, Instant lastSeenAt) {}

    /** @brief Thread-safe map tracking active in-flight generation leases by user ID. */
    private final ConcurrentHashMap<UUID, ActiveGeneration> activeGenerations = new ConcurrentHashMap<>();

    /** @brief Thread-safe map tracking active device bindings by user ID. */
    private final ConcurrentHashMap<UUID, DeviceBinding> deviceBindings = new ConcurrentHashMap<>();

    /**
     * @brief Verifies that the incoming request originates from the user's bound generation device.
     * @param userId Unique identifier of the authenticated user.
     * @param deviceId Client device fingerprint or UUID identifier.
     * @throws ForbiddenException if user attempts generation from an unauthorized secondary device.
     */
    public void verifyDevice(UUID userId, String deviceId) {
        if (userId == null) {
            return;
        }
        String normalizedDeviceId = (deviceId != null && !deviceId.isBlank()) ? deviceId.trim() : "default_device";
        Instant now = Instant.now();

        deviceBindings.compute(userId, (k, current) -> {
            if (current == null || Duration.between(current.lastSeenAt(), now).compareTo(DEVICE_SESSION_TTL) > 0) {
                log.info("Binding user {} to generation device: {}", userId, normalizedDeviceId);
                return new DeviceBinding(normalizedDeviceId, now, now);
            }

            if (!current.deviceId().equals(normalizedDeviceId)) {
                log.warn("Device mismatch for user {}: bound to '{}', attempted from '{}'",
                    userId, current.deviceId(), normalizedDeviceId);
                throw new ForbiddenException(
                    "AI generation is locked to your active device to prevent account sharing and abuse. " +
                    "Please generate from your primary device or wait for the session to expire."
                );
            }

            return new DeviceBinding(current.deviceId(), current.boundAt(), now);
        });
    }

    /**
     * @brief Acquires an exclusive in-flight generation lock for the user.
     * @param userId Unique identifier of the user initiating generation.
     * @param deviceId Client device identifier.
     * @throws QuotaExceededException if a generation is already running for this user account.
     */
    public void acquireGenerationLock(UUID userId, String deviceId) {
        if (userId == null) {
            return;
        }
        String normalizedDeviceId = (deviceId != null && !deviceId.isBlank()) ? deviceId.trim() : "default_device";
        Instant now = Instant.now();

        activeGenerations.compute(userId, (k, existing) -> {
            if (existing != null && Duration.between(existing.acquiredAt(), now).compareTo(LEASE_TIMEOUT) <= 0) {
                log.warn("Concurrent generation blocked for user {}. Active lease acquired at {}",
                    userId, existing.acquiredAt());
                throw new QuotaExceededException(
                    "Another AI task generation is currently in progress for your account. " +
                    "Parallel generation requests are forbidden to prevent system abuse. Please wait for it to complete."
                );
            }
            return new ActiveGeneration(userId, normalizedDeviceId, now);
        });
    }

    /**
     * @brief Releases the in-flight generation lock for the given user upon completion or failure.
     * @param userId Unique identifier of the user.
     */
    public void releaseGenerationLock(UUID userId) {
        if (userId != null) {
            activeGenerations.remove(userId);
        }
    }

    /**
     * @brief Clears all device bindings and active generation locks (useful for testing).
     */
    public void clear() {
        activeGenerations.clear();
        deviceBindings.clear();
    }
}
