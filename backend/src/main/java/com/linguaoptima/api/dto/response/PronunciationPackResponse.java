/**
 * @file PronunciationPackResponse.java
 * @brief Response DTO representing a CEFR-calibrated pronunciation drill pack.
 */
package com.linguaoptima.api.dto.response;

import com.linguaoptima.api.domain.enums.CefrLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @brief Response DTO containing target phonemes, drill vocabulary, and minimal pairs for speech tasks.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PronunciationPackResponse {

    /** @brief Unique identifier of the pronunciation pack (e.g. "A1_DENTAL_FRICATIVES"). */
    private String id;

    /** @brief Target CEFR language proficiency benchmark. */
    private CefrLevel cefrLevel;

    /** @brief Phonetic category category discriminator. */
    private String category;

    /** @brief Human-readable descriptive title for educators and students. */
    private String title;

    /** @brief Target IPA phonemes focused in this pack (e.g. ["/θ/", "/ð/"]). */
    private List<String> phonemes;

    /** @brief Level-calibrated target drill words. */
    private List<String> drillWords;

    /** @brief Curated minimal pair contrasts demonstrating key sound differentiations. */
    private List<MinimalPairDto> minimalPairs;

    /** @brief Pedagogical guidance on articulation, tongue positioning, and voicing. */
    private String contrastFocus;

    /** @brief Practice sentence or tongue twister emphasizing rhythm and target sounds. */
    private String tongueTwister;
}
