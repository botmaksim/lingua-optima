/**
 * @file TaskParamsRequest.java
 * @brief Request DTO specifying CEFR level, topic, domain, and type for AI task generation.
 */
package com.linguaoptima.api.dto.request;

import com.linguaoptima.api.domain.enums.AIProvider;
import com.linguaoptima.api.domain.enums.CefrLevel;
import com.linguaoptima.api.domain.enums.DifficultyLevel;
import com.linguaoptima.api.domain.enums.TaskType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * @brief Request DTO specifying CEFR level, topic, domain, and type for AI task generation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskParamsRequest {

    /** @brief Field representing cefr level in TaskParamsRequest. */
    @NotNull(message = "CEFR level is required")
    private CefrLevel cefrLevel;

    /** @brief Field representing grammar topic in TaskParamsRequest. */
    private String grammarTopic;

    /** @brief Field representing domain in TaskParamsRequest. */
    private String domain;

    /** @brief Field representing task type in TaskParamsRequest. */
    @NotNull(message = "Task type is required")
    private TaskType taskType;

    /** @brief Field representing difficulty in TaskParamsRequest. */
    @Builder.Default
    private DifficultyLevel difficulty = DifficultyLevel.MEDIUM;

    /** @brief Field representing number of questions in TaskParamsRequest. */
    @Builder.Default
    private int numberOfQuestions = 5;

    /** @brief Field representing total points / max score in TaskParamsRequest. */
    @Builder.Default
    private Integer totalPoints = 100;

    /** @brief Field representing provider in TaskParamsRequest. */
    private AIProvider provider;

    /** @brief Field representing model name in TaskParamsRequest. */
    private String modelName;

    /** @brief Optional user-provided custom grammar rules, formulas, or teaching notes. */
    private String customRule;

    /** @brief Optional user-provided custom target vocabulary words, idioms, or collocations. */
    private String customVocabulary;

    /** @brief Optional server path of uploaded custom grammar rule file. */
    private String ruleFilePath;

    /** @brief Optional server path of uploaded custom vocabulary reference file. */
    private String vocabularyFilePath;

    /** @brief Optional Eco Mode flag to skip verbose rule and vocabulary context to conserve prompt tokens. */
    private Boolean ecoMode;

    /** @brief Optional target cohort group IDs for direct multi-group assignment upon generation. */
    private List<UUID> groupIds;

    /** @brief Optional submission due date when assigning to cohorts upon generation. */
    private LocalDateTime dueDate;

    /** @brief Optional maximum allowed attempts when assigning to cohorts upon generation. */
    private Integer maxAttempts;

    /** @brief Optional reading assignment title. */
    private String readingTitle;

    /** @brief Optional reading passage length or volume (e.g. SHORT, MEDIUM, LONG, or specific word count). */
    private String readingLength;

    /** @brief Optional description of paragraph count or structure for the reading passage. */
    private String readingParagraphs;

    /** @brief Optional count of multiple choice questions to generate for reading tasks. */
    private Integer mcqQuestionsCount;

    /** @brief Optional points allocated per multiple choice question in reading tasks. */
    private Integer mcqPoints;

    /** @brief Optional count of True/False questions to generate for reading tasks. */
    private Integer trueFalseQuestionsCount;

    /** @brief Optional points allocated per True/False question in reading tasks. */
    private Integer trueFalsePoints;

    /** @brief Optional count of sentence completion / open questions to generate for reading tasks. */
    private Integer openQuestionsCount;

    /** @brief Optional points allocated per sentence completion / open question in reading tasks. */
    private Integer openPoints;

    /** @brief Optional speaking and pronunciation task title. */
    private String speakingTitle;

    /** @brief Optional target sounds or phonemes to practice (e.g. /θ/, /ð/, /r/ vs /l/, /v/ vs /w/). */
    private String targetSounds;

    /** @brief Optional length preset for speaking task (e.g. SHORT, MEDIUM, LONG, CUSTOM). */
    private String speakingLength;
}
