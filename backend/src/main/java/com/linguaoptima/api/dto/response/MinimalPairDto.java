/**
 * @file MinimalPairDto.java
 * @brief DTO representing a phonetic minimal pair contrast for pronunciation training.
 */
package com.linguaoptima.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @brief Phonetic minimal pair contrast containing two words distinguishing target phonemes.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MinimalPairDto {

    /** @brief First contrast word (e.g. "think"). */
    private String word1;

    /** @brief Second contrast word (e.g. "sink"). */
    private String word2;

    /** @brief Target IPA phoneme in first word (e.g. "/θ/"). */
    private String phoneme1;

    /** @brief Contrast IPA phoneme in second word (e.g. "/s/"). */
    private String phoneme2;
}
