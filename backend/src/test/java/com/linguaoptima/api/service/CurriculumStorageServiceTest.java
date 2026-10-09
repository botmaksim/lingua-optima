/**
 * @file CurriculumStorageServiceTest.java
 * @brief Unit tests for CurriculumStorageService verifying file persistence, extension guards, and reference resolution.
 */
package com.linguaoptima.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.domain.enums.CefrLevel;
import com.linguaoptima.api.domain.enums.CurriculumFileType;
import com.linguaoptima.api.dto.response.CurriculumReferenceResponse;
import com.linguaoptima.api.dto.response.CurriculumUploadResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @brief Unit tests for CurriculumStorageService.
 */
class CurriculumStorageServiceTest {

    @TempDir
    Path tempDir;

    private CurriculumStorageService storageService;
    private ObjectMapper objectMapper;
    private User testUser;

    @BeforeEach
    void setUp() throws IOException {
        objectMapper = new ObjectMapper();
        storageService = new CurriculumStorageService(tempDir.toString(), objectMapper);
        storageService.init();

        testUser = User.builder()
            .id(UUID.randomUUID())
            .email("student@lingua.com")
            .build();

        // Seed sample canonical files in temp directory
        Path rulesDir = tempDir.resolve("rules");
        Files.createDirectories(rulesDir);
        Files.writeString(rulesDir.resolve("mixed_conditionals.md"), "If had V3, would V1");
        Files.writeString(rulesDir.resolve("inversion_and_cleft.md"), "Not only did he... but also");

        Path vocabDir = tempDir.resolve("vocabulary");
        Files.createDirectories(vocabDir);
        Files.writeString(vocabDir.resolve("mixed_conditionals.json"), "{\"words\":[\"consequence\",\"hypothetical\"]}");
        Files.writeString(vocabDir.resolve("inversion_and_cleft.json"), "{\"words\":[\"seldom\",\"scarcely\"]}");
        Files.writeString(vocabDir.resolve("business_advanced.json"), "{\"words\":[\"synergy\",\"leverage\"]}");
        Files.writeString(vocabDir.resolve("academic_collocations.json"), "{\"words\":[\"substantiate\",\"empirical\"]}");
        Files.writeString(rulesDir.resolve("present_simple_to_be_common_verbs.md"), "Subject + V1 (s/es)");
        Files.writeString(vocabDir.resolve("present_simple_to_be_common_verbs.json"), "{\"words\":[\"always\",\"usually\"]}");
    }

    @Test
    void testInitCreatesDirectories() {
        assertTrue(Files.exists(tempDir.resolve("rules")));
        assertTrue(Files.exists(tempDir.resolve("vocabulary")));
        assertTrue(Files.exists(tempDir.resolve("custom/rules")));
        assertTrue(Files.exists(tempDir.resolve("custom/vocabulary")));
    }

    @Test
    void testSaveCurriculumFileValidationFailures() {
        // Null file
        assertThrows(IllegalArgumentException.class, () ->
            storageService.saveCurriculumFile(null, CurriculumFileType.RULE, "Topic", testUser));

        // Empty file
        MockMultipartFile emptyFile = new MockMultipartFile("file", "rule.txt", "text/plain", new byte[0]);
        assertThrows(IllegalArgumentException.class, () ->
            storageService.saveCurriculumFile(emptyFile, CurriculumFileType.RULE, "Topic", testUser));

        // File too large (> 2 MB)
        byte[] largeBytes = new byte[(int) (2 * 1024 * 1024 + 10)];
        MockMultipartFile largeFile = new MockMultipartFile("file", "large.md", "text/markdown", largeBytes);
        assertThrows(IllegalArgumentException.class, () ->
            storageService.saveCurriculumFile(largeFile, CurriculumFileType.RULE, "Topic", testUser));

        // Unsupported extension
        MockMultipartFile exeFile = new MockMultipartFile("file", "danger.exe", "application/octet-stream", "bad".getBytes(StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class, () ->
            storageService.saveCurriculumFile(exeFile, CurriculumFileType.RULE, "Topic", testUser));
    }

    @Test
    void testSaveCurriculumFileSuccess() {
        String ruleText = "Custom Inversion Rule: Seldom + Aux + Subject + Verb.";
        MockMultipartFile ruleFile = new MockMultipartFile(
            "file",
            "custom_inversion.md",
            "text/markdown",
            ruleText.getBytes(StandardCharsets.UTF_8)
        );

        CurriculumUploadResponse response = storageService.saveCurriculumFile(
            ruleFile,
            CurriculumFileType.RULE,
            "Advanced Inversion",
            testUser
        );

        assertNotNull(response);
        assertEquals("custom_inversion.md", response.getFileName());
        assertEquals(CurriculumFileType.RULE, response.getFileType());
        assertEquals(ruleText, response.getFullContent());
        assertTrue(response.getContentSnippet().contains("Custom Inversion Rule"));
        assertEquals("Advanced Inversion", response.getTopic());
        assertNotNull(response.getServerPath());

        // Test vocabulary file upload
        String vocabJson = "{\"words\":[\"resilient\",\"ubiquitous\"]}";
        MockMultipartFile vocabFile = new MockMultipartFile(
            "file",
            "vocab.json",
            "application/json",
            vocabJson.getBytes(StandardCharsets.UTF_8)
        );

        CurriculumUploadResponse vocabResponse = storageService.saveCurriculumFile(
            vocabFile,
            CurriculumFileType.VOCABULARY,
            "Custom Vocab",
            null
        );

        assertEquals(CurriculumFileType.VOCABULARY, vocabResponse.getFileType());
        assertEquals(vocabJson, vocabResponse.getFullContent());
    }

    @Test
    void testGetReferenceCurriculumCanonicalAndSynthesized() {
        // Conditionals canonical (exact slug)
        CurriculumReferenceResponse condRef = storageService.getReferenceCurriculum(CefrLevel.B2, "Mixed Conditionals");
        assertEquals("CANONICAL", condRef.getSource());
        assertTrue(condRef.getReferenceRule().contains("If had V3"));
        assertTrue(condRef.getReferenceVocabulary().contains("consequence"));

        // Conditionals canonical (keyword fallback)
        CurriculumReferenceResponse kwCondRef = storageService.getReferenceCurriculum(CefrLevel.B2, "Advanced Conditional Forms");
        assertEquals("CANONICAL", kwCondRef.getSource());
        assertTrue(kwCondRef.getReferenceRule().contains("If had V3"));

        // Inversion canonical
        CurriculumReferenceResponse invRef = storageService.getReferenceCurriculum(CefrLevel.C1, "Cleft Sentences & Inversion");
        assertEquals("CANONICAL", invRef.getSource());
        assertTrue(invRef.getReferenceRule().contains("Not only did he"));

        // Business canonical
        CurriculumReferenceResponse busRef = storageService.getReferenceCurriculum(CefrLevel.B2, "Business Negotiations");
        assertEquals("CANONICAL", busRef.getSource());
        assertTrue(busRef.getReferenceVocabulary().contains("synergy"));

        // Academic canonical
        CurriculumReferenceResponse acadRef = storageService.getReferenceCurriculum(CefrLevel.C1, "Academic Research Paper");
        assertEquals("CANONICAL", acadRef.getSource());
        assertTrue(acadRef.getReferenceVocabulary().contains("substantiate"));

        // Synthesized fallback
        CurriculumReferenceResponse synthRef = storageService.getReferenceCurriculum(CefrLevel.A2, "Past Simple Irregular Verbs");
        assertEquals("SYNTHESIZED", synthRef.getSource());
        assertTrue(synthRef.getReferenceRule().contains("Past Simple Irregular Verbs"));
        assertTrue(synthRef.getReferenceVocabulary().contains("demonstrate"));

        // Null level and topic
        CurriculumReferenceResponse nullRef = storageService.getReferenceCurriculum(null, null);
        assertEquals("B1", nullRef.getCefrLevel());
        assertEquals("SYNTHESIZED", nullRef.getSource());
    }

    @Test
    void testResolvePromptCurriculumContext() {
        // Explicit custom rule and vocab provided
        String[] explicit = storageService.resolvePromptCurriculumContext(
            CefrLevel.B2,
            "Custom Topic",
            "My explicit rule",
            "word1, word2"
        );
        assertEquals("My explicit rule", explicit[0]);
        assertEquals("word1, word2", explicit[1]);

        // Null custom rule and vocab -> falls back to reference
        String[] fallback = storageService.resolvePromptCurriculumContext(
            CefrLevel.B2,
            "Mixed Conditionals",
            null,
            ""
        );
        assertNotNull(fallback[0]);
        assertTrue(fallback[0].contains("If had V3"));
        assertNotNull(fallback[1]);
        assertTrue(fallback[1].contains("consequence"));
    }

    @Test
    void testInitFailsGracefullyOnIOException() throws IOException {
        Path regularFile = tempDir.resolve("blocked-file");
        Files.writeString(regularFile, "blocking");
        CurriculumStorageService brokenStorage = new CurriculumStorageService(
            regularFile.resolve("sub").toString(),
            objectMapper
        );
        assertDoesNotThrow(brokenStorage::init);
    }

    @Test
    void testSaveCurriculumFileEdgeCases() throws IOException {
        // Path traversal in filename
        MockMultipartFile traversalFile = new MockMultipartFile(
            "file", "../traversal.txt", "text/plain", "Rule".getBytes(StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class, () ->
            storageService.saveCurriculumFile(traversalFile, CurriculumFileType.RULE, "Topic", testUser));

        // Missing extension
        MockMultipartFile noExtFile = new MockMultipartFile(
            "file", "noextension", "text/plain", "Rule".getBytes(StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class, () ->
            storageService.saveCurriculumFile(noExtFile, CurriculumFileType.RULE, "Topic", testUser));

        // Null filename defaults to curriculum.txt
        MockMultipartFile nullNameFile = new MockMultipartFile(
            "file", null, "text/plain", "Default name content".getBytes(StandardCharsets.UTF_8));
        CurriculumUploadResponse nullNameRes = storageService.saveCurriculumFile(
            nullNameFile, CurriculumFileType.RULE, null, testUser);
        assertNotNull(nullNameRes);
        assertEquals("curriculum.txt", nullNameRes.getFileName());
        assertNull(nullNameRes.getTopic());

        // File exceeding 300 chars gets snippet truncated
        String longText = "A".repeat(350);
        MockMultipartFile longFile = new MockMultipartFile(
            "file", "long.txt", "text/plain", longText.getBytes(StandardCharsets.UTF_8));
        CurriculumUploadResponse longRes = storageService.saveCurriculumFile(
            longFile, CurriculumFileType.RULE, "Long Topic", testUser);
        assertTrue(longRes.getContentSnippet().endsWith("..."));
        assertEquals(303, longRes.getContentSnippet().length());

        // IOException when reading file bytes
        org.springframework.web.multipart.MultipartFile failingFile = org.mockito.Mockito.mock(org.springframework.web.multipart.MultipartFile.class);
        org.mockito.Mockito.when(failingFile.isEmpty()).thenReturn(false);
        org.mockito.Mockito.when(failingFile.getSize()).thenReturn(10L);
        org.mockito.Mockito.when(failingFile.getOriginalFilename()).thenReturn("valid.txt");
        org.mockito.Mockito.when(failingFile.getBytes()).thenThrow(new IOException("Disk read error"));
        assertThrows(RuntimeException.class, () ->
            storageService.saveCurriculumFile(failingFile, CurriculumFileType.RULE, "Topic", testUser));
    }

    @Test
    void testCanonicalReadExceptionHandling() throws IOException {
        // Test readCanonicalFile exception by having a directory instead of a regular file
        Path rulesDir = tempDir.resolve("rules");
        Path fakeFileAsDir = rulesDir.resolve("inversion_and_cleft.md");
        Files.deleteIfExists(fakeFileAsDir);
        Files.createDirectory(fakeFileAsDir);

        CurriculumReferenceResponse invRef = storageService.getReferenceCurriculum(CefrLevel.C1, "Cleft Sentences & Inversion");
        assertNotNull(invRef);
        assertEquals("SYNTHESIZED", invRef.getSource());

        // Test readCanonicalVocabulary exception with invalid JSON
        Path vocabDir = tempDir.resolve("vocabulary");
        Path invalidJsonFile = vocabDir.resolve("academic_collocations.json");
        Files.writeString(invalidJsonFile, "{not-valid-json!!!");

        CurriculumReferenceResponse acadRef = storageService.getReferenceCurriculum(CefrLevel.C1, "Academic Research Paper");
        assertNotNull(acadRef);
        assertTrue(acadRef.getReferenceVocabulary().contains("essential"));

        // Test canonical file not existing (fallthrough return null / default list)
        Files.deleteIfExists(rulesDir.resolve("mixed_conditionals.md"));
        CurriculumReferenceResponse missingCondRef = storageService.getReferenceCurriculum(CefrLevel.B2, "Mixed Conditionals");
        assertNotNull(missingCondRef);
        assertEquals("SYNTHESIZED", missingCondRef.getSource());

        Files.deleteIfExists(vocabDir.resolve("business_advanced.json"));
        CurriculumReferenceResponse missingBusRef = storageService.getReferenceCurriculum(CefrLevel.B2, "Business Negotiations");
        assertNotNull(missingBusRef);
        assertTrue(missingBusRef.getReferenceVocabulary().contains("essential"));
    }

    @Test
    void testToSlugHelper() {
        assertEquals("general", CurriculumStorageService.toSlug(null));
        assertEquals("general", CurriculumStorageService.toSlug(""));
        assertEquals("general", CurriculumStorageService.toSlug("   "));
        assertEquals("present_simple_to_be_common_verbs",
            CurriculumStorageService.toSlug("Present Simple (to be & common verbs)"));
    }

    @Test
    void testSlugBasedCanonicalResolution() {
        CurriculumReferenceResponse ref = storageService.getReferenceCurriculum(
            CefrLevel.A1, "Present Simple (to be & common verbs)"
        );
        assertNotNull(ref);
        assertEquals("CANONICAL", ref.getSource());
        assertEquals("Subject + V1 (s/es)", ref.getReferenceRule());
        assertNotNull(ref.getReferenceVocabulary());
        assertTrue(ref.getReferenceVocabulary().contains("always"));
    }
}
