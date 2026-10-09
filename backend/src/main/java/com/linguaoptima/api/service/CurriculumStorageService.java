/**
 * @file CurriculumStorageService.java
 * @brief Service managing server-side storage and context resolution for grammar rules and target vocabulary.
 */
package com.linguaoptima.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linguaoptima.api.domain.User;
import com.linguaoptima.api.domain.enums.CefrLevel;
import com.linguaoptima.api.domain.enums.CurriculumFileType;
import com.linguaoptima.api.dto.response.CurriculumReferenceResponse;
import com.linguaoptima.api.dto.response.CurriculumUploadResponse;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

/**
 * @brief Manages server-side file persistence and AI context resolution for curriculum materials.
 */
@Service
@Slf4j
public class CurriculumStorageService {

    /** @brief Maximum allowed upload size for curriculum text files (2 MB). */
    public static final long MAX_FILE_SIZE_BYTES = 2 * 1024 * 1024;

    /** @brief Allowed file extensions for uploaded curriculum materials. */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".txt", ".md", ".json", ".csv");

    /** @brief Root directory for persisted curriculum materials. */
    private final Path rootStoragePath;

    /** @brief Jackson object mapper for JSON parsing. */
    private final ObjectMapper objectMapper;

    /**
     * @brief Constructs CurriculumStorageService with configured storage location.
     * @param storagePath Root folder path configured via application properties.
     * @param objectMapper Injected JSON object mapper.
     */
    public CurriculumStorageService(@Value("${app.curriculum.storage-path:./data/curriculum}") String storagePath,
                                    ObjectMapper objectMapper) {
        this.rootStoragePath = Paths.get(storagePath).toAbsolutePath().normalize();
        this.objectMapper = objectMapper;
    }

    /**
     * @brief Initializes directory tree for canonical and custom curriculum uploads.
     */
    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(rootStoragePath.resolve("rules"));
            Files.createDirectories(rootStoragePath.resolve("vocabulary"));
            Files.createDirectories(rootStoragePath.resolve("custom/rules"));
            Files.createDirectories(rootStoragePath.resolve("custom/vocabulary"));
            log.info("Initialized curriculum storage at: {}", rootStoragePath);
        } catch (IOException e) {
            log.error("Failed to initialize curriculum storage directories: {}", e.getMessage());
        }
    }

    /**
     * @brief Persists an uploaded grammar rule or vocabulary reference file to server storage.
     * @param file Uploaded multipart file.
     * @param type Discriminator indicating rule or vocabulary file.
     * @param topic Optional topic associated with the curriculum file.
     * @param user Authenticated user uploading the file.
     * @return CurriculumUploadResponse containing extracted snippet and server path.
     * @throws IllegalArgumentException if file validation fails.
     */
    public CurriculumUploadResponse saveCurriculumFile(MultipartFile file,
                                                      CurriculumFileType type,
                                                      String topic,
                                                      User user) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Upload file cannot be null or empty.");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("File size exceeds 2 MB limit.");
        }

        String rawName = file.getOriginalFilename();
        String originalName = (rawName != null && !rawName.isBlank()) ? rawName : "curriculum.txt";
        if (originalName.contains("..") || originalName.contains("/") || originalName.contains("\\")) {
            throw new IllegalArgumentException("Invalid file destination path.");
        }
        String ext = getFileExtension(originalName).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException("Unsupported file format '" + ext + "'. Allowed: " + ALLOWED_EXTENSIONS);
        }

        String sanitizedBase = originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
        String uniqueName = UUID.randomUUID() + "_" + sanitizedBase;

        String subfolder = (type == CurriculumFileType.RULE) ? "custom/rules" : "custom/vocabulary";
        Path targetDir = rootStoragePath.resolve(subfolder);
        Path targetFile = targetDir.resolve(uniqueName);

        try {
            byte[] bytes = file.getBytes();
            Files.write(targetFile, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            String fullContent = new String(bytes, StandardCharsets.UTF_8).trim();

            String snippet = fullContent.length() > 300
                ? fullContent.substring(0, 300) + "..."
                : fullContent;

            log.info("User {} saved custom curriculum file: {}", user != null ? user.getEmail() : "anonymous", uniqueName);

            return CurriculumUploadResponse.builder()
                .fileName(originalName)
                .fileType(type)
                .fileSize(file.getSize())
                .contentSnippet(snippet)
                .fullContent(fullContent)
                .serverPath(rootStoragePath.relativize(targetFile).toString())
                .topic(topic != null ? topic.trim() : null)
                .build();
        } catch (IOException e) {
            log.error("Failed to persist curriculum upload: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save curriculum file: " + e.getMessage(), e);
        }
    }

    /**
     * @brief Resolves target grammar rule and target vocabulary strings to inject into the AI task generation prompt.
     * @param level Target CEFR benchmark.
     * @param topic Grammar topic or custom practice subject.
     * @param customRule User-supplied custom rule text.
     * @param customVocabulary User-supplied custom vocabulary text.
     * @return Array of two Strings: [resolvedTargetRule, resolvedTargetVocabulary].
     */
    public String[] resolvePromptCurriculumContext(CefrLevel level, String topic, String customRule, String customVocabulary) {
        String resolvedRule = (customRule != null && !customRule.isBlank()) ? customRule.trim() : null;
        String resolvedVocab = (customVocabulary != null && !customVocabulary.isBlank()) ? customVocabulary.trim() : null;

        if (resolvedRule == null || resolvedVocab == null) {
            CurriculumReferenceResponse ref = getReferenceCurriculum(level, topic);
            if (resolvedRule == null && ref.getReferenceRule() != null) {
                resolvedRule = ref.getReferenceRule();
            }
            if (resolvedVocab == null && ref.getReferenceVocabulary() != null && !ref.getReferenceVocabulary().isEmpty()) {
                resolvedVocab = String.join(", ", ref.getReferenceVocabulary());
            }
        }

        return new String[]{resolvedRule, resolvedVocab};
    }

    /**
     * @brief Normalizes a topic string into a filesystem-safe canonical slug.
     * @param topic Topic title string.
     * @return Normalized slug string.
     */
    public static String toSlug(String topic) {
        if (topic == null || topic.isBlank()) {
            return "general";
        }
        return topic.toLowerCase()
            .replaceAll("[^a-z0-9]+", "_")
            .replaceAll("^_+|_+$", "");
    }

    /**
     * @brief Retrieves canonical or synthesized pedagogical reference materials for a given CEFR level and topic.
     * @param level Target CEFR level.
     * @param topic Subject or grammar topic.
     * @return CurriculumReferenceResponse containing rule explanation and vocabulary list.
     */
    public CurriculumReferenceResponse getReferenceCurriculum(CefrLevel level, String topic) {
        String safeLevel = (level != null) ? level.name() : "B1";
        String safeTopic = (topic != null && !topic.isBlank()) ? topic.trim() : "General English Grammar";

        // 1. Check for exact or normalized slug match in canonical files
        String slug = toSlug(safeTopic);
        String rule = readCanonicalFile("rules/" + slug + ".md");
        List<String> vocab = readCanonicalVocabulary("vocabulary/" + slug + ".json");
        if (rule != null) {
            return CurriculumReferenceResponse.builder()
                .cefrLevel(safeLevel)
                .grammarTopic(safeTopic)
                .referenceRule(rule)
                .referenceVocabulary(vocab)
                .source("CANONICAL")
                .build();
        }

        // 2. Check for pre-seeded canonical files matching topic keywords
        String lowerTopic = safeTopic.toLowerCase();
        if (lowerTopic.contains("conditional")) {
            String condRule = readCanonicalFile("rules/mixed_conditionals.md");
            if (condRule != null) {
                return CurriculumReferenceResponse.builder()
                    .cefrLevel(safeLevel)
                    .grammarTopic(safeTopic)
                    .referenceRule(condRule)
                    .referenceVocabulary(List.of("consequence", "hypothetical", "implication", "contingency", "speculate"))
                    .source("CANONICAL")
                    .build();
            }
        }

        if (lowerTopic.contains("inversion") || lowerTopic.contains("cleft")) {
            String invRule = readCanonicalFile("rules/inversion_and_cleft.md");
            if (invRule != null) {
                return CurriculumReferenceResponse.builder()
                    .cefrLevel(safeLevel)
                    .grammarTopic(safeTopic)
                    .referenceRule(invRule)
                    .referenceVocabulary(List.of("seldom", "scarcely", "paramount", "pivotal", "substantiate"))
                    .source("CANONICAL")
                    .build();
            }
        }

        if (lowerTopic.contains("business") || lowerTopic.contains("work")) {
            List<String> busVocab = readCanonicalVocabulary("vocabulary/business_advanced.json");
            return CurriculumReferenceResponse.builder()
                .cefrLevel(safeLevel)
                .grammarTopic(safeTopic)
                .referenceRule("Focus on polite professional modals, indirect questions, and passive voice in corporate discourse.")
                .referenceVocabulary(busVocab)
                .source("CANONICAL")
                .build();
        }

        if (lowerTopic.contains("academic") || lowerTopic.contains("science")) {
            List<String> acadVocab = readCanonicalVocabulary("vocabulary/academic_collocations.json");
            return CurriculumReferenceResponse.builder()
                .cefrLevel(safeLevel)
                .grammarTopic(safeTopic)
                .referenceRule("Focus on hedging devices, impersonal passive constructions, and complex participle clauses.")
                .referenceVocabulary(acadVocab)
                .source("CANONICAL")
                .build();
        }

        // 2. Synthesize pedagogical reference
        return CurriculumReferenceResponse.builder()
            .cefrLevel(safeLevel)
            .grammarTopic(safeTopic)
            .referenceRule("Target Rule: Master accuracy in " + safeTopic + " aligned with CEFR " + safeLevel + " standards.")
            .referenceVocabulary(List.of("demonstrate", "illustrate", "clarify", "emphasize", "integrate"))
            .source("SYNTHESIZED")
            .build();
    }

    /**
     * @brief Reads a canonical rule markdown file from root storage path.
     * @param relativePath Relative file path.
     * @return File content or null if not found.
     */
    private String readCanonicalFile(String relativePath) {
        try {
            Path file = rootStoragePath.resolve(relativePath);
            if (Files.exists(file)) {
                return Files.readString(file, StandardCharsets.UTF_8).trim();
            }
        } catch (Exception ignored) {
            log.trace("Unable to read canonical file: {}", relativePath);
        }
        return null;
    }

    /**
     * @brief Reads canonical vocabulary word lists from JSON file in root storage path.
     * @param relativePath Relative file path.
     * @return List of vocabulary words or fallback default list.
     */
    private List<String> readCanonicalVocabulary(String relativePath) {
        try {
            Path file = rootStoragePath.resolve(relativePath);
            if (Files.exists(file)) {
                JsonNode root = objectMapper.readTree(file.toFile());
                List<String> words = new ArrayList<>();
                if (root.has("words")) {
                    for (JsonNode w : root.path("words")) {
                        words.add(w.asText());
                    }
                }
                if (!words.isEmpty()) {
                    return words;
                }
            }
        } catch (Exception ignored) {
            log.trace("Unable to read canonical vocabulary: {}", relativePath);
        }
        return List.of("essential", "comprehensive", "effective", "perspectives", "context");
    }

    /**
     * @brief Extracts lowercase file extension including leading period.
     * @param fileName Full file name.
     * @return Extension string (e.g. .md) or empty string.
     */
    private String getFileExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return (dot >= 0) ? fileName.substring(dot) : "";
    }
}
