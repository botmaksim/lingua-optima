/**
 * @file GenerationProtectionServiceTest.java
 * @brief Unit tests for GenerationProtectionService covering seamless multi-device handover and concurrency locks.
 */
package com.linguaoptima.api.service;

import com.linguaoptima.api.dto.response.GenerationStatusResponse;
import com.linguaoptima.api.exception.ConcurrentGenerationException;
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
    void testVerifyDevice_IdleDevicesRecordPresenceWithoutThrowing() {
        // Laptop records presence
        assertDoesNotThrow(() -> service.verifyDevice(userId, "dev-laptop"));

        // Different device (phone) can also record presence without 403 Forbidden
        assertDoesNotThrow(() -> service.verifyDevice(userId, "dev-phone"));

        // Default device when null/blank
        assertDoesNotThrow(() -> service.verifyDevice(userId, null));
        assertDoesNotThrow(() -> service.verifyDevice(userId, "   "));
    }

    @Test
    void testAcquireGenerationLock_NullUserIdDoesNothing() {
        assertNull(service.acquireGenerationLock(null, "dev-1"));
        assertDoesNotThrow(() -> service.releaseGenerationLock(null));
        assertDoesNotThrow(() -> service.releaseGenerationLock(null, "lease-1"));
    }

    @Test
    void testAcquireGenerationLock_SequentialAcrossDifferentDevicesWhenIdleSucceeds() {
        // 1. Laptop acquires lock and completes
        String lease1 = service.acquireGenerationLock(userId, "dev-laptop");
        assertNotNull(lease1);
        service.releaseGenerationLock(userId, lease1);

        // 2. Phone immediately acquires lock smoothly without ban
        String lease2 = service.acquireGenerationLock(userId, "dev-phone");
        assertNotNull(lease2);
        assertNotEquals(lease1, lease2);
        service.releaseGenerationLock(userId, lease2);
    }

    @Test
    void testAcquireGenerationLock_ConcurrentSameDeviceThrowsException() {
        String lease = service.acquireGenerationLock(userId, "dev-1");
        assertNotNull(lease);

        ConcurrentGenerationException ex = assertThrows(ConcurrentGenerationException.class, () ->
            service.acquireGenerationLock(userId, "dev-1")
        );
        assertTrue(ex.getMessage().contains("already in progress on this device"));
        assertEquals("dev-1", ex.getActiveDeviceId());
    }

    @Test
    void testAcquireGenerationLock_ConcurrentDifferentDeviceThrowsException() {
        String lease = service.acquireGenerationLock(userId, "dev-laptop");
        assertNotNull(lease);

        ConcurrentGenerationException ex = assertThrows(ConcurrentGenerationException.class, () ->
            service.acquireGenerationLock(userId, "dev-phone")
        );
        assertTrue(ex.getMessage().contains("in progress on another device"));
        assertEquals("dev-laptop", ex.getActiveDeviceId());
        assertTrue(ex.isCanTakeover());
    }

    @Test
    void testTakeoverGeneration_TransfersLockAndAllowsImmediateAcquisition() {
        // Laptop starts generation
        String laptopLease = service.acquireGenerationLock(userId, "dev-laptop");
        assertNotNull(laptopLease);

        // Phone requests takeover
        service.takeoverGeneration(userId, "dev-phone");

        // Phone can now acquire lock immediately
        String phoneLease = service.acquireGenerationLock(userId, "dev-phone");
        assertNotNull(phoneLease);

        // Laptop trying to release its old superseded lease does not disrupt phone's lease
        service.releaseGenerationLock(userId, laptopLease);

        GenerationStatusResponse status = service.getStatus(userId, "dev-phone");
        assertTrue(status.isGenerating());
        assertTrue(status.isCurrentDevice());
        assertEquals("dev-phone", status.getActiveDeviceId());

        // Phone cleanly releases its own lease
        service.releaseGenerationLock(userId, phoneLease);
        GenerationStatusResponse finalStatus = service.getStatus(userId, "dev-phone");
        assertFalse(finalStatus.isGenerating());
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

        assertEquals(userId, current.userId());
        assertEquals("dev-1", current.deviceId());
        assertNotNull(current.leaseId());
        assertNotNull(current.acquiredAt());

        // Age beyond LEASE_TIMEOUT (60 seconds)
        Instant twoMinutesAgo = Instant.now().minusSeconds(100);
        active.put(userId, new GenerationProtectionService.ActiveGeneration(userId, "dev-1", "old-lease", twoMinutesAgo));

        // Acquisition now succeeds as old lease expired
        assertDoesNotThrow(() -> service.acquireGenerationLock(userId, "dev-1"));
    }

    @Test
    void testGetStatus_IdleAndActiveState() {
        GenerationStatusResponse idle = service.getStatus(userId, "dev-phone");
        assertFalse(idle.isGenerating());
        assertNull(idle.getActiveDeviceId());
        assertFalse(idle.isCurrentDevice());

        service.acquireGenerationLock(userId, "dev-phone");
        GenerationStatusResponse activeCurrent = service.getStatus(userId, "dev-phone");
        assertTrue(activeCurrent.isGenerating());
        assertEquals("dev-phone", activeCurrent.getActiveDeviceId());
        assertTrue(activeCurrent.isCurrentDevice());

        GenerationStatusResponse activeOther = service.getStatus(userId, "dev-laptop");
        assertTrue(activeOther.isGenerating());
        assertEquals("dev-phone", activeOther.getActiveDeviceId());
        assertFalse(activeOther.isCurrentDevice());
    }

    @Test
    void testClear() {
        service.verifyDevice(userId, "dev-1");
        service.acquireGenerationLock(userId, "dev-1");
        service.clear();

        GenerationStatusResponse status = service.getStatus(userId, "dev-1");
        assertFalse(status.isGenerating());
    }
}
