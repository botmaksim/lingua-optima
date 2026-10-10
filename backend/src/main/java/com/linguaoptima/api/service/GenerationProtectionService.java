/**
 * @file GenerationProtectionService.java
 * @brief Manages concurrency locks, seamless device handover, and in-flight lease lifecycle for AI generation.
 */
package com.linguaoptima.api.service;

import com.linguaoptima.api.dto.response.GenerationStatusResponse;
import com.linguaoptima.api.exception.ConcurrentGenerationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @brief Coordinates AI task generation leases per user account.
 *
 * Guarantees that:
 * 1. A user account can be logged in across multiple devices concurrently.
 * 2. Only one AI generation request runs at any instant per account to prevent concurrency abuse.
 * 3. When idle, any authorized device can generate immediately without rigid lockouts.
 * 4. In-flight generations can be smoothly transferred/taken over by another device on demand.
 */
@Service
@Slf4j
public class GenerationProtectionService {

    /** @brief Safety lease timeout after which an in-flight generation lock automatically expires. */
    public static final Duration LEASE_TIMEOUT = Duration.ofSeconds(60);

    /** @brief Inactivity period after which a bound device session expires. */
    public static final Duration DEVICE_SESSION_TTL = Duration.ofHours(12);

    /** @brief Record capturing an active generation lock lease. */
    public record ActiveGeneration(UUID userId, String deviceId, String leaseId, Instant acquiredAt) {}

    /** @brief Record capturing a user's bound device session. */
    public record DeviceBinding(String deviceId, Instant boundAt, Instant lastSeenAt) {}

    /** @brief Thread-safe map tracking active in-flight generation leases by user ID. */
    private final ConcurrentHashMap<UUID, ActiveGeneration> activeGenerations = new ConcurrentHashMap<>();

    /** @brief Thread-safe map tracking active device bindings by user ID. */
    private final ConcurrentHashMap<UUID, DeviceBinding> deviceBindings = new ConcurrentHashMap<>();

    /**
     * @brief Records device presence and updates last seen timestamp without blocking idle devices.
     * @param userId Unique identifier of the authenticated user.
     * @param deviceId Client device fingerprint or UUID identifier.
     */
    public void verifyDevice(UUID userId, String deviceId) {
        if (userId == null) {
            return;
        }
        String normalizedDeviceId = (deviceId != null && !deviceId.isBlank()) ? deviceId.trim() : "default_device";
        Instant now = Instant.now();

        deviceBindings.compute(userId, (k, current) -> {
            if (current == null) {
                return new DeviceBinding(normalizedDeviceId, now, now);
            }
            return new DeviceBinding(current.deviceId(), current.boundAt(), now);
        });
    }

    /**
     * @brief Acquires an exclusive in-flight generation lock for the user and returns the lease ID.
     * @param userId Unique identifier of the user initiating generation.
     * @param deviceId Client device identifier.
     * @return Generated lease token identifier.
     * @throws ConcurrentGenerationException if another generation is currently in progress.
     */
    public String acquireGenerationLock(UUID userId, String deviceId) {
        if (userId == null) {
            return null;
        }
        String normalizedDeviceId = (deviceId != null && !deviceId.isBlank()) ? deviceId.trim() : "default_device";
        Instant now = Instant.now();
        String leaseId = UUID.randomUUID().toString();

        activeGenerations.compute(userId, (k, existing) -> {
            if (existing != null && Duration.between(existing.acquiredAt(), now).compareTo(LEASE_TIMEOUT) <= 0) {
                if (existing.deviceId().equals(normalizedDeviceId)) {
                    log.warn("Concurrent generation blocked on same device for user {}", userId);
                    throw new ConcurrentGenerationException(
                        "An AI task generation is already in progress on this device. Please wait for it to complete.",
                        existing.deviceId()
                    );
                } else {
                    log.warn("Concurrent generation on secondary device for user {}: active={}, requested={}",
                        userId, existing.deviceId(), normalizedDeviceId);
                    throw new ConcurrentGenerationException(
                        "AI task generation is currently in progress on another device. " +
                        "You can transfer generation control to this device.",
                        existing.deviceId()
                    );
                }
            }
            deviceBindings.put(userId, new DeviceBinding(normalizedDeviceId, now, now));
            return new ActiveGeneration(userId, normalizedDeviceId, leaseId, now);
        });

        return leaseId;
    }

    /**
     * @brief Releases the in-flight generation lock if the provided leaseId matches.
     * @param userId Unique identifier of the user.
     * @param leaseId Lease token returned during acquisition (or null to force release).
     */
    public void releaseGenerationLock(UUID userId, String leaseId) {
        if (userId == null) {
            return;
        }
        activeGenerations.computeIfPresent(userId, (k, current) -> {
            if (leaseId == null || leaseId.equals(current.leaseId())) {
                return null;
            }
            return current;
        });
    }

    /**
     * @brief Releases the in-flight generation lock for the given user upon completion or failure.
     * @param userId Unique identifier of the user.
     */
    public void releaseGenerationLock(UUID userId) {
        releaseGenerationLock(userId, null);
    }

    /**
     * @brief Forcefully transfers generation control to the caller's device, releasing any active lease.
     * @param userId Unique identifier of the user.
     * @param targetDeviceId Device requesting to take over control.
     */
    public void takeoverGeneration(UUID userId, String targetDeviceId) {
        if (userId == null) {
            return;
        }
        String normalizedDeviceId = (targetDeviceId != null && !targetDeviceId.isBlank())
            ? targetDeviceId.trim()
            : "default_device";
        Instant now = Instant.now();

        activeGenerations.remove(userId);
        deviceBindings.put(userId, new DeviceBinding(normalizedDeviceId, now, now));
        log.info("Generation control for user {} transferred to device {}", userId, normalizedDeviceId);
    }

    /**
     * @brief Inspects current AI generation status for the user.
     * @param userId Unique identifier of the user.
     * @param currentDeviceId Device requesting the status inspection.
     * @return GenerationStatusResponse containing active status.
     */
    public GenerationStatusResponse getStatus(UUID userId, String currentDeviceId) {
        if (userId == null) {
            return new GenerationStatusResponse(false, null, false);
        }
        String normalizedDeviceId = (currentDeviceId != null && !currentDeviceId.isBlank())
            ? currentDeviceId.trim()
            : "default_device";
        Instant now = Instant.now();
        ActiveGeneration active = activeGenerations.get(userId);

        boolean isGenerating = active != null && Duration.between(active.acquiredAt(), now).compareTo(LEASE_TIMEOUT) <= 0;
        String activeDevice = isGenerating ? active.deviceId() : null;
        boolean isCurrent = isGenerating && normalizedDeviceId.equals(activeDevice);

        return GenerationStatusResponse.builder()
            .isGenerating(isGenerating)
            .activeDeviceId(activeDevice)
            .isCurrentDevice(isCurrent)
            .build();
    }

    /**
     * @brief Clears all device bindings and active generation locks (useful for testing).
     */
    public void clear() {
        activeGenerations.clear();
        deviceBindings.clear();
    }
}
