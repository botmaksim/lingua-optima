/**
 * @file PronunciationPackServiceTest.java
 * @brief Unit tests for PronunciationPackService verifying JSON loading and phonetic matching.
 */
package com.linguaoptima.api.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linguaoptima.api.domain.enums.CefrLevel;
import com.linguaoptima.api.dto.response.PronunciationPackResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

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
        // Test null level fallback to all packs
        List<PronunciationPackResponse> allFromNull = service.getPacksByLevel(null);
        assertEquals(36, allFromNull.size());

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

        // Null and blank ID
        assertTrue(service.getPackById(null).isEmpty());
        assertTrue(service.getPackById("   ").isEmpty());

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

        // Raw IPA phoneme without slashes
        PronunciationPackResponse ipaPack = service.resolvePack(CefrLevel.B1, "practice with θ sound");
        assertNotNull(ipaPack);
        assertTrue(ipaPack.getId().contains("DENTAL"));

        // Multi-char phoneme like iː
        PronunciationPackResponse vowelPack = service.resolvePack(CefrLevel.A1, "focus on iː vs ɪ");
        assertNotNull(vowelPack);
        assertTrue(vowelPack.getId().contains("VOWEL"));

        // Long vs short vowel phrase
        PronunciationPackResponse longShort = service.findMatchingPack(CefrLevel.A1, "long vs short");
        assertNotNull(longShort);

        // Direct ID match in query
        PronunciationPackResponse directId = service.findMatchingPack(CefrLevel.A1, "please practice a1_dental_fricatives today");
        assertNotNull(directId);
        assertEquals("A1_DENTAL_FRICATIVES", directId.getId());
    }

    @Test
    void testResolvePackByKeyword() {
        PronunciationPackResponse liquidPack = service.resolvePack(CefrLevel.A2, "liquids r vs l");
        assertNotNull(liquidPack);
        assertEquals(CefrLevel.A2, liquidPack.getCefrLevel());
        assertTrue(liquidPack.getId().contains("LIQUIDS"));

        // Test r & l variant
        PronunciationPackResponse rAndL = service.findMatchingPack(CefrLevel.A2, "practice r & l");
        assertNotNull(rAndL);

        PronunciationPackResponse labialPack = service.resolvePack(CefrLevel.B1, "/v/ vs /w/");
        assertNotNull(labialPack);
        assertTrue(labialPack.getId().contains("LABIALS"));

        // Test v & w variant
        PronunciationPackResponse vAndW = service.findMatchingPack(CefrLevel.B1, "v & w contrast");
        assertNotNull(vAndW);

        // Test th- variant
        PronunciationPackResponse thVariant = service.findMatchingPack(CefrLevel.B1, "words starting with th-");
        assertNotNull(thVariant);

        // Sibilants
        PronunciationPackResponse sibilantPack = service.resolvePack(CefrLevel.B1, "affricate and sibilant sounds");
        assertNotNull(sibilantPack);
        assertTrue(sibilantPack.getId().contains("SIBILANTS"));

        PronunciationPackResponse affricateSlash = service.findMatchingPack(CefrLevel.B1, "sound /ʃ/ and /tʃ/");
        assertNotNull(affricateSlash);

        // Silent letters & reduction
        PronunciationPackResponse silentPack = service.resolvePack(CefrLevel.B2, "silent letters and reduction");
        assertNotNull(silentPack);
        assertTrue(silentPack.getId().contains("SILENT"));

        // Connected speech / rhythm
        PronunciationPackResponse connectedPack = service.resolvePack(CefrLevel.B2, "connected speech rhythm and linking");
        assertNotNull(connectedPack);
        assertTrue(connectedPack.getId().contains("CONNECTED"));
    }

    @Test
    void testResolvePackWithExplicitPackId() {
        PronunciationPackResponse pack = service.resolvePack(CefrLevel.C1, "C1_SIBILANTS", null);
        assertNotNull(pack);
        assertEquals("C1_SIBILANTS", pack.getId());

        // Unknown packId falls through to findMatchingPack
        PronunciationPackResponse fallbackPack = service.resolvePack(CefrLevel.C1, "UNKNOWN_PACK_ID", "connected");
        assertNotNull(fallbackPack);
        assertEquals(CefrLevel.C1, fallbackPack.getCefrLevel());
    }

    @Test
    void testResolvePackByTitleKeywords() {
        // Direct title keyword match via containsAnyTerm
        PronunciationPackResponse pack = service.resolvePack(CefrLevel.B1, "thoroughly articulation");
        assertNotNull(pack);
    }

    @Test
    void testResolvePackGlobalCategoryMatch() {
        // Search for a category not in A1, fallback to global match across all packs
        PronunciationPackResponse pack = service.findMatchingPack(CefrLevel.A1, "reduction irregular silent");
        assertNotNull(pack);
    }

    @Test
    void testFallbackWhenTargetSoundsEmptyOrUnknown() {
        // Null and blank targetSounds
        PronunciationPackResponse packNull = service.resolvePack(CefrLevel.B2, null, null);
        assertNotNull(packNull);
        assertEquals(CefrLevel.B2, packNull.getCefrLevel());

        PronunciationPackResponse packBlank = service.resolvePack(CefrLevel.B2, null, "   ");
        assertNotNull(packBlank);
        assertEquals(CefrLevel.B2, packBlank.getCefrLevel());

        // Default null level
        PronunciationPackResponse packNullLevel = service.findMatchingPack(null, null);
        assertNotNull(packNullLevel);
        assertEquals(CefrLevel.B1, packNullLevel.getCefrLevel());

        // Target sounds with no matching keywords -> returns level default
        PronunciationPackResponse unknownQuery = service.resolvePack(CefrLevel.A2, "xyzqwe123456789");
        assertNotNull(unknownQuery);
        assertEquals(CefrLevel.A2, unknownQuery.getCefrLevel());
    }

    @Test
    void testInitMissingResourceHandlesGracefully() {
        ResourceLoader mockLoader = mock(ResourceLoader.class);
        Resource mockResource = mock(Resource.class);
        when(mockResource.exists()).thenReturn(false);
        when(mockLoader.getResource(anyString())).thenReturn(mockResource);

        PronunciationPackService emptyService = new PronunciationPackService(new ObjectMapper(), mockLoader);
        assertDoesNotThrow(emptyService::init);
        assertTrue(emptyService.getAllPacks().isEmpty());
        assertNull(emptyService.findMatchingPack(CefrLevel.B1, "anything"));
    }

    @Test
    void testInitCorruptResourceHandlesGracefully() throws Exception {
        ResourceLoader mockLoader = mock(ResourceLoader.class);
        Resource mockResource = mock(Resource.class);
        when(mockResource.exists()).thenReturn(true);
        when(mockResource.getInputStream()).thenThrow(new IOException("Disk read error"));
        when(mockLoader.getResource(anyString())).thenReturn(mockResource);

        PronunciationPackService corruptService = new PronunciationPackService(new ObjectMapper(), mockLoader);
        assertDoesNotThrow(corruptService::init);
        assertTrue(corruptService.getAllPacks().isEmpty());
    }

    @Test
    void testComprehensivePhoneticAndCategoryBranches() {
        // Test all phonetic tokens and category triggers
        assertNotNull(service.findMatchingPack(CefrLevel.A1, "dental fricative"));
        assertNotNull(service.findMatchingPack(CefrLevel.A1, "sound /θ/"));
        assertNotNull(service.findMatchingPack(CefrLevel.A2, "liquid consonants"));
        assertNotNull(service.findMatchingPack(CefrLevel.A2, "/r/ sound"));
        assertNotNull(service.findMatchingPack(CefrLevel.B1, "labial articulation"));
        assertNotNull(service.findMatchingPack(CefrLevel.B1, "sound /v/"));
        assertNotNull(service.findMatchingPack(CefrLevel.A1, "vowel contrasts"));
        assertNotNull(service.findMatchingPack(CefrLevel.A1, "/iː/ vowels"));
        assertNotNull(service.findMatchingPack(CefrLevel.B1, "sibilant consonants"));
        assertNotNull(service.findMatchingPack(CefrLevel.B1, "/ʃ/ vs /tʃ/"));
        assertNotNull(service.findMatchingPack(CefrLevel.B2, "silent patterns"));
        assertNotNull(service.findMatchingPack(CefrLevel.B2, "irregular word forms"));
        assertNotNull(service.findMatchingPack(CefrLevel.B2, "reduction in speech"));
        assertNotNull(service.findMatchingPack(CefrLevel.B2, "connected speech flow"));
        assertNotNull(service.findMatchingPack(CefrLevel.B2, "sentence rhythm"));
        assertNotNull(service.findMatchingPack(CefrLevel.B2, "word stress"));
        assertNotNull(service.findMatchingPack(CefrLevel.B2, "linking words"));

        // Single letter token match
        assertNotNull(service.findMatchingPack(CefrLevel.A2, "focus on sound r"));
        assertNotNull(service.findMatchingPack(CefrLevel.B1, "focus on sound v"));

        // Direct title / term match
        assertNotNull(service.findMatchingPack(CefrLevel.B1, "fricatives and plosives"));
    }
}
