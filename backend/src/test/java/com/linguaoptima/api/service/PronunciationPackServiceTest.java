/**
 * @file PronunciationPackServiceTest.java
 * @brief Unit tests for PronunciationPackService verifying JSON loading and phonetic matching.
 */
package com.linguaoptima.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linguaoptima.api.domain.enums.CefrLevel;
import com.linguaoptima.api.dto.response.PronunciationPackResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @brief Unit tests for PronunciationPackService.
 */
class PronunciationPackServiceTest {

    private PronunciationPackService service;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        DefaultResourceLoader resourceLoader = new DefaultResourceLoader();
        service = new PronunciationPackService(objectMapper, resourceLoader);
        service.init();
    }

    @Test
    void testLoadsAllPacksSuccessfully() {
        List<PronunciationPackResponse> allPacks = service.getAllPacks();
        assertNotNull(allPacks);
        assertFalse(allPacks.isEmpty());
        assertEquals(36, allPacks.size(), "Should load exactly 36 pre-calibrated pronunciation packs");
    }

    @Test
    void testGetPacksByLevel() {
        for (CefrLevel level : CefrLevel.values()) {
            List<PronunciationPackResponse> packs = service.getPacksByLevel(level);
            assertNotNull(packs);
            assertFalse(packs.isEmpty(), "Level " + level + " should have pronunciation packs");
            assertTrue(packs.stream().allMatch(p -> p.getCefrLevel() == level));
        }
    }

    @Test
    void testGetPackById() {
        Optional<PronunciationPackResponse> packOpt = service.getPackById("A1_DENTAL_FRICATIVES");
        assertTrue(packOpt.isPresent());
        PronunciationPackResponse pack = packOpt.get();
        assertEquals("A1_DENTAL_FRICATIVES", pack.getId());
        assertEquals(CefrLevel.A1, pack.getCefrLevel());
        assertNotNull(pack.getDrillWords());
        assertFalse(pack.getDrillWords().isEmpty());
        assertNotNull(pack.getMinimalPairs());
        assertFalse(pack.getMinimalPairs().isEmpty());

        // Case-insensitive lookup
        Optional<PronunciationPackResponse> lowerCaseOpt = service.getPackById("a1_dental_fricatives");
        assertTrue(lowerCaseOpt.isPresent());
        assertEquals(pack.getId(), lowerCaseOpt.get().getId());

        // Non-existent ID
        Optional<PronunciationPackResponse> unknown = service.getPackById("NON_EXISTENT");
        assertTrue(unknown.isEmpty());
    }

    @Test
    void testResolvePackByPhoneme() {
        PronunciationPackResponse pack = service.resolvePack(CefrLevel.B1, "/θ/ and /ð/");
        assertNotNull(pack);
        assertEquals(CefrLevel.B1, pack.getCefrLevel());
        assertTrue(pack.getId().contains("DENTAL"));
    }

    @Test
    void testResolvePackByKeyword() {
        PronunciationPackResponse liquidPack = service.resolvePack(CefrLevel.A2, "liquids r vs l");
        assertNotNull(liquidPack);
        assertEquals(CefrLevel.A2, liquidPack.getCefrLevel());
        assertTrue(liquidPack.getId().contains("LIQUIDS"));

        PronunciationPackResponse labialPack = service.resolvePack(CefrLevel.B1, "/v/ vs /w/");
        assertNotNull(labialPack);
        assertTrue(labialPack.getId().contains("LABIALS"));
    }

    @Test
    void testResolvePackWithExplicitPackId() {
        PronunciationPackResponse pack = service.resolvePack(CefrLevel.C1, "C1_SIBILANTS", null);
        assertNotNull(pack);
        assertEquals("C1_SIBILANTS", pack.getId());
    }

    @Test
    void testFallbackWhenTargetSoundsEmpty() {
        PronunciationPackResponse pack = service.resolvePack(CefrLevel.B2, null, null);
        assertNotNull(pack);
        assertEquals(CefrLevel.B2, pack.getCefrLevel());
    }
}
