/**
 * @file GenerationProtectionServiceTest.java
 * @brief Unit tests for GenerationProtectionService covering single-device binding and concurrency locks.
 */
package com.linguaoptima.api.service;

import com.linguaoptima.api.exception.ForbiddenException;
import com.linguaoptima.api.exception.QuotaExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class GenerationProtectionServiceTest {

    private GenerationProtectionService service;
    private UUID userId;

    @BeforeEach
    void setUp() {
        service = new GenerationProtectionService();
        service.clear();
        userId = UUID.randomUUID();
    }

    @Test
    void testVerifyDevice_NullUserIdDoesNothing() {
        assertDoesNotThrow(() -> service.verifyDevice(null, "dev-1"));
    }

    @Test
    void testVerifyDevice_InitialBindingAndSubsequentSuccess() {
        // First binding with custom device id
        assertDoesNotThrow(() -> service.verifyDevice(userId, "dev-laptop"));

        // Same device continues to succeed
        assertDoesNotThrow(() -> service.verifyDevice(userId, "dev-laptop"));
    }

    @Test
    void testVerifyDevice_DefaultDeviceWhenBlank() {
        assertDoesNotThrow(() -> service.verifyDevice(userId, null));
        assertDoesNotThrow(() -> service.verifyDevice(userId, "   "));
        assertDoesNotThrow(() -> service.verifyDevice(userId, "default_device"));
    }

    @Test
    void testVerifyDevice_MismatchThrowsForbiddenException() {
        service.verifyDevice(userId, "primary-laptop");

        ForbiddenException ex = assertThrows(ForbiddenException.class, () ->
            service.verifyDevice(userId, "secondary-phone")
        );
        assertTrue(ex.getMessage().contains("AI generation is locked to your active device"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testVerifyDevice_SessionExpirationAllowsNewDevice() {
        service.verifyDevice(userId, "primary-laptop");

        // Manually age the binding past DEVICE_SESSION_TTL (12 hours)
        Map<UUID, GenerationProtectionService.DeviceBinding> bindings =
            (Map<UUID, GenerationProtectionService.DeviceBinding>) ReflectionTestUtils.getField(service, "deviceBindings");
        assertNotNull(bindings);
        GenerationProtectionService.DeviceBinding current = bindings.get(userId);
        assertNotNull(current);

        // Verify record methods
        assertEquals("primary-laptop", current.deviceId());
        assertNotNull(current.boundAt());
        assertNotNull(current.lastSeenAt());

        Instant twelveHoursAgo = Instant.now().minusSeconds(13 * 3600);
        bindings.put(userId, new GenerationProtectionService.DeviceBinding("primary-laptop", twelveHoursAgo, twelveHoursAgo));

        // Now binding from secondary device should succeed as old session expired
        assertDoesNotThrow(() -> service.verifyDevice(userId, "secondary-phone"));
    }

    @Test
    void testAcquireGenerationLock_NullUserIdDoesNothing() {
        assertDoesNotThrow(() -> service.acquireGenerationLock(null, "dev-1"));
        assertDoesNotThrow(() -> service.releaseGenerationLock(null));
    }

    @Test
    void testAcquireGenerationLock_SequentialSuccessAfterRelease() {
        service.acquireGenerationLock(userId, "dev-1");

        // Releasing frees the lease
        service.releaseGenerationLock(userId);

        // Immediate acquisition succeeds
        assertDoesNotThrow(() -> service.acquireGenerationLock(userId, "dev-1"));
    }

    @Test
    void testAcquireGenerationLock_ConcurrentThrowsQuotaExceededException() {
        service.acquireGenerationLock(userId, "dev-1");

        QuotaExceededException ex = assertThrows(QuotaExceededException.class, () ->
            service.acquireGenerationLock(userId, "dev-1")
        );
        assertTrue(ex.getMessage().contains("Another AI task generation is currently in progress"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testAcquireGenerationLock_TimeoutAllowsNewLease() {
        service.acquireGenerationLock(userId, "dev-1");

        Map<UUID, GenerationProtectionService.ActiveGeneration> active =
            (Map<UUID, GenerationProtectionService.ActiveGeneration>) ReflectionTestUtils.getField(service, "activeGenerations");
        assertNotNull(active);
        GenerationProtectionService.ActiveGeneration current = active.get(userId);
        assertNotNull(current);

        // Verify record methods
        assertEquals(userId, current.userId());
        assertEquals("dev-1", current.deviceId());
        assertNotNull(current.acquiredAt());

        // Age beyond LEASE_TIMEOUT (90 seconds)
        Instant twoMinutesAgo = Instant.now().minusSeconds(100);
        active.put(userId, new GenerationProtectionService.ActiveGeneration(userId, "dev-1", twoMinutesAgo));

        // Acquisition now succeeds as old lease expired
        assertDoesNotThrow(() -> service.acquireGenerationLock(userId, "dev-1"));
    }

    @Test
    void testClear() {
        service.verifyDevice(userId, "dev-1");
        service.acquireGenerationLock(userId, "dev-1");

        service.clear();

        // After clearing, user can bind another device and acquire lock
        assertDoesNotThrow(() -> service.verifyDevice(userId, "dev-2"));
        assertDoesNotThrow(() -> service.acquireGenerationLock(userId, "dev-2"));
    }
}
