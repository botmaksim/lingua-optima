/**
 * @file PronunciationPackService.java
 * @brief Service responsible for loading and resolving CEFR-calibrated pronunciation drill packs.
 */
package com.linguaoptima.api.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linguaoptima.api.domain.enums.CefrLevel;
import com.linguaoptima.api.dto.response.PronunciationPackResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @brief Service managing structured phonetic drill packs and minimal pair contrasts from JSON resources.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PronunciationPackService {

    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;

    private static final String PACKS_RESOURCE_PATH = "classpath:phonetics/pronunciation_packs.json";

    private final List<PronunciationPackResponse> cachedPacks = new ArrayList<>();
    private final Map<String, PronunciationPackResponse> packsById = new HashMap<>();
    private final Map<CefrLevel, List<PronunciationPackResponse>> packsByLevel = new EnumMap<>(CefrLevel.class);

    /**
     * @brief Initializes and loads pronunciation packs from classpath JSON.
     */
    @PostConstruct
    public void init() {
        try {
            Resource resource = resourceLoader.getResource(PACKS_RESOURCE_PATH);
            if (!resource.exists()) {
                log.warn("Pronunciation packs resource not found at {}", PACKS_RESOURCE_PATH);
                return;
            }

            try (InputStream is = resource.getInputStream()) {
                List<PronunciationPackResponse> packs = objectMapper.readValue(
                    is,
                    new TypeReference<List<PronunciationPackResponse>>() {}
                );

                cachedPacks.clear();
                packsById.clear();
                packsByLevel.clear();

                for (PronunciationPackResponse pack : packs) {
                    cachedPacks.add(pack);
                    if (pack.getId() != null) {
                        packsById.put(pack.getId().toUpperCase(), pack);
                    }
                    if (pack.getCefrLevel() != null) {
                        packsByLevel.computeIfAbsent(pack.getCefrLevel(), k -> new ArrayList<>()).add(pack);
                    }
                }
                log.info("Successfully loaded {} pronunciation packs across {} CEFR levels",
                    cachedPacks.size(), packsByLevel.size());
            }
        } catch (Exception e) {
            log.error("Failed to load pronunciation packs from {}: {}", PACKS_RESOURCE_PATH, e.getMessage(), e);
        }
    }

    /**
     * @brief Returns all loaded pronunciation packs.
     * @return Immutable copy of all packs.
     */
    public List<PronunciationPackResponse> getAllPacks() {
        return Collections.unmodifiableList(cachedPacks);
    }

    /**
     * @brief Returns pronunciation packs filtered by CEFR level.
     * @param level Target CEFR level.
     * @return List of packs for the given level.
     */
    public List<PronunciationPackResponse> getPacksByLevel(CefrLevel level) {
        if (level == null) {
            return getAllPacks();
        }
        return Collections.unmodifiableList(packsByLevel.getOrDefault(level, Collections.emptyList()));
    }

    /**
     * @brief Retrieves a specific pack by unique identifier.
     * @param id Pack ID (case-insensitive).
     * @return Optional containing the pack if found.
     */
    public Optional<PronunciationPackResponse> getPackById(String id) {
        if (id == null || id.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(packsById.get(id.trim().toUpperCase()));
    }

    /**
     * @brief Resolves the best matching pack by pack ID or target sound string and CEFR level.
     * @param level Target CEFR level.
     * @param packId Optional pack ID.
     * @param targetSounds Optional target sounds or phoneme string.
     * @return Resolved pronunciation pack or safe fallback.
     */
    public PronunciationPackResponse resolvePack(CefrLevel level, String packId, String targetSounds) {
        if (packId != null && !packId.isBlank()) {
            Optional<PronunciationPackResponse> byId = getPackById(packId);
            if (byId.isPresent()) {
                return byId.get();
            }
        }
        return findMatchingPack(level, targetSounds);
    }

    /**
     * @brief Resolves the best matching pack by target sounds string and CEFR level.
     * @param level Target CEFR level.
     * @param targetSounds Optional target sounds or phoneme description.
     * @return Resolved pronunciation pack or safe fallback.
     */
    public PronunciationPackResponse resolvePack(CefrLevel level, String targetSounds) {
        return resolvePack(level, null, targetSounds);
    }

    /**
     * @brief Matches target sound input against cached packs for the specified CEFR level.
     * @param level Target CEFR level.
     * @param targetSounds Target sound description or phoneme input.
     * @return Matching pack or default pack for the level.
     */
    public PronunciationPackResponse findMatchingPack(CefrLevel level, String targetSounds) {
        CefrLevel targetLevel = level != null ? level : CefrLevel.B1;
        List<PronunciationPackResponse> levelPacks = packsByLevel.getOrDefault(targetLevel, cachedPacks);

        if (levelPacks.isEmpty() && !cachedPacks.isEmpty()) {
            levelPacks = cachedPacks;
        }

        if (targetSounds == null || targetSounds.isBlank()) {
            return levelPacks.isEmpty() ? null : levelPacks.get(0);
        }

        String query = targetSounds.toLowerCase();

        // 1. Direct ID match for level
        for (PronunciationPackResponse pack : levelPacks) {
            if (pack.getId() != null && query.contains(pack.getId().toLowerCase())) {
                return pack;
            }
        }

        // 2. Phoneme match for level (e.g. "θ", "ð", "ʃ", "tʃ", "iː", "ɪ")
        for (PronunciationPackResponse pack : levelPacks) {
            if (pack.getPhonemes() != null) {
                for (String phoneme : pack.getPhonemes()) {
                    String clean = phoneme.replace("/", "").toLowerCase();
                    if (query.contains(phoneme.toLowerCase()) || (!clean.isBlank() && query.contains(clean))) {
                        return pack;
                    }
                }
            }
        }

        // 3. Category keywords match for level
        for (PronunciationPackResponse pack : levelPacks) {
            String category = pack.getCategory() != null ? pack.getCategory().toLowerCase() : "";
            if (matchesCategory(query, category)) {
                return pack;
            }
        }

        // 4. Title / example keyword match for level
        for (PronunciationPackResponse pack : levelPacks) {
            if (pack.getTitle() != null && containsAnyTerm(query, pack.getTitle().toLowerCase())) {
                return pack;
            }
        }

        // 5. Global match across all levels if not found in level
        for (PronunciationPackResponse pack : cachedPacks) {
            if (pack.getCategory() != null && matchesCategory(query, pack.getCategory().toLowerCase())) {
                return pack;
            }
        }

        // Fallback: first pack for the level
        return levelPacks.isEmpty() ? null : levelPacks.get(0);
    }

    private boolean matchesCategory(String query, String category) {
        if (category.contains("dental") && (query.contains("dental") || query.contains("th-") || query.contains("/θ/"))) {
            return true;
        }
        if (category.contains("liquid") && (query.contains("liquid") || query.contains("/r/") || query.contains("r vs l") || query.contains("r & l"))) {
            return true;
        }
        if (category.contains("labial") && (query.contains("labial") || query.contains("/v/") || query.contains("v vs w") || query.contains("v & w"))) {
            return true;
        }
        if (category.contains("vowel") && (query.contains("vowel") || query.contains("long vs short") || query.contains("/iː/"))) {
            return true;
        }
        if (category.contains("sibilant") && (query.contains("sibilant") || query.contains("affricate") || query.contains("/ʃ/") || query.contains("/tʃ/"))) {
            return true;
        }
        if (category.contains("silent") && (query.contains("silent") || query.contains("irregular") || query.contains("reduction"))) {
            return true;
        }
        if (category.contains("connected") && (query.contains("connected") || query.contains("rhythm") || query.contains("stress") || query.contains("linking"))) {
            return true;
        }
        return category.length() > 3 && query.contains(category);
    }

    private boolean containsAnyTerm(String query, String text) {
        String[] parts = text.split("[\\s,;:/]+");
        for (String part : parts) {
            if (part.length() > 3 && query.contains(part)) {
                return true;
            }
        }
        return false;
    }
}
