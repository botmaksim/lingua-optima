/**
 * @file PromptTemplatesTest.java
 * @brief Unit and slice test suite for PromptTemplates.
 */
package com.linguaoptima.api.util;

import com.linguaoptima.api.domain.enums.CefrLevel;
import com.linguaoptima.api.dto.request.TaskParamsRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @brief Unit and slice test suite for PromptTemplates.
 */
class PromptTemplatesTest {

    /**
     * @brief Verifies unit test scenario: prompt templates.
     */
    @Test
    void testPromptTemplates() {
        String taskPrompt = PromptTemplates.buildTaskGenerationPrompt("B1", "Past Simple", "Work", "MCQ", "MEDIUM", 5);
        assertTrue(taskPrompt.contains("B1"));
        assertTrue(taskPrompt.contains("Past Simple"));
        assertTrue(taskPrompt.contains("MCQ"));

        String essayPrompt = PromptTemplates.buildEssayScoringPrompt("B2", "Essay text here");
        assertTrue(essayPrompt.contains("B2"));
        assertTrue(essayPrompt.contains("Essay text here"));
        assertTrue(essayPrompt.contains("taskAchievement"));

        // Test essay scoring prompt with topic, targetRule, and targetVocabulary
        String essayEnforcedPrompt = PromptTemplates.buildEssayScoringPrompt(
            "C1", "Essay with inversion...", "Impact of AI", "Inversion with Seldom/Hardly", "ubiquitous, indispensable"
        );
        assertTrue(essayEnforcedPrompt.contains("ASSIGNED ESSAY TOPIC / PROMPT:"));
        assertTrue(essayEnforcedPrompt.contains("Impact of AI"));
        assertTrue(essayEnforcedPrompt.contains("TARGET GRAMMAR RULES TO BE ACTIVELY DEMONSTRATED:"));
        assertTrue(essayEnforcedPrompt.contains("Inversion with Seldom/Hardly"));
        assertTrue(essayEnforcedPrompt.contains("TARGET VOCABULARY WORDS & COLLOCATIONS TO INCORPORATE:"));
        assertTrue(essayEnforcedPrompt.contains("ubiquitous, indispensable"));
        assertTrue(essayEnforcedPrompt.contains("TARGET RULES & VOCABULARY EVALUATION CONTRACT:"));
        assertTrue(essayEnforcedPrompt.contains("Penalize the \"grammarRange\" and \"taskAchievement\" scores"));

        String grammarPrompt = PromptTemplates.buildGrammarCheckingPrompt("Student text", "Answer key");
        assertTrue(grammarPrompt.contains("Student text"));
        assertTrue(grammarPrompt.contains("Answer key"));
        String essayGenPrompt = PromptTemplates.buildTaskGenerationPrompt("C1", "Cleft Sentences", "Technology", "ESSAY", "HARD", 5);
        assertTrue(essayGenPrompt.contains("C1"));
        assertTrue(essayGenPrompt.contains("Cleft Sentences"));
        assertTrue(essayGenPrompt.contains("Technology"));
        assertTrue(essayGenPrompt.contains("ESSAY writing assignment"));
        assertTrue(essayGenPrompt.contains("guiding discussion prompts"));

        // Test with custom targetRule and targetVocabulary
        String customPrompt = PromptTemplates.buildTaskGenerationPrompt(
            "B2", "Mixed Conditionals", "Business", "MCQ", "MEDIUM", 5,
            "If + had + V3, would + V1", "mitigate, lucrative"
        );
        assertTrue(customPrompt.contains("TARGET GRAMMAR RULE"));
        assertTrue(customPrompt.contains("If + had + V3, would + V1"));
        assertTrue(customPrompt.contains("TARGET VOCABULARY"));
        assertTrue(customPrompt.contains("mitigate, lucrative"));

        // Test essay with targetRule and targetVocabulary
        String customEssayPrompt = PromptTemplates.buildTaskGenerationPrompt(
            "C1", "Advanced Inversion", "Academic", "ESSAY", "HARD", 3,
            "Seldom + aux + subj + verb", "empirical, substantiate"
        );
        assertTrue(customEssayPrompt.contains("Seldom + aux + subj + verb"));
        assertTrue(customEssayPrompt.contains("empirical, substantiate"));

        // Test REWRITE prompt template
        String rewritePrompt = PromptTemplates.buildTaskGenerationPrompt(
            "B2", "Inversion", "Literature", "REWRITE", "HARD", 4
        );
        assertTrue(rewritePrompt.contains("SENTENCE REWRITING"));
        assertTrue(rewritePrompt.contains("REWRITE (Sentence Transformation)"));
        assertTrue(rewritePrompt.contains("options\" array MUST be empty []"));

        // Test OPEN_BRACKETS prompt template
        String openBracketsPrompt = PromptTemplates.buildTaskGenerationPrompt(
            "B1", "Conditionals", "Everyday Life", "OPEN_BRACKETS", "MEDIUM", 5
        );
        assertTrue(openBracketsPrompt.contains("раскрытие скобок"));
        assertTrue(openBracketsPrompt.contains("OPEN_BRACKETS"));
        assertTrue(openBracketsPrompt.contains("options\" array MUST be empty []"));

        // Test READING prompt template
        String readingPrompt = PromptTemplates.buildTaskGenerationPrompt(
            "B2", "Environment", "Science", "READING", "MEDIUM", 7
        );
        assertTrue(readingPrompt.contains("Reading Comprehension assignment"));
        assertTrue(readingPrompt.contains("B2"));
        assertTrue(readingPrompt.contains("Environment"));
    }

    /**
     * @brief Tests buildReadingGenerationPrompt with TaskParamsRequest and various configurations.
     */
    @Test
    void testBuildReadingGenerationPromptWithParams() {
        com.linguaoptima.api.dto.request.TaskParamsRequest params = new com.linguaoptima.api.dto.request.TaskParamsRequest();
        params.setCefrLevel(CefrLevel.B2);
        params.setGrammarTopic("Renewable Energy");
        params.setDomain("Technology");
        params.setDifficulty(com.linguaoptima.api.domain.enums.DifficultyLevel.HARD);
        params.setReadingTitle("The Green Transition");
        params.setReadingLength("Long (400-600 words)");
        params.setReadingParagraphs("4 paragraphs focusing on solar, wind, and storage");
        params.setMcqQuestionsCount(4);
        params.setMcqPoints(15);
        params.setTrueFalseQuestionsCount(3);
        params.setTrueFalsePoints(10);
        params.setOpenQuestionsCount(2);
        params.setOpenPoints(20);

        String prompt = PromptTemplates.buildReadingGenerationPrompt(params, null, "solar, wind, grid");
        assertTrue(prompt.contains("The Green Transition"));
        assertTrue(prompt.contains("solar, wind, grid"));
        assertTrue(prompt.contains("Renewable Energy"));
        assertTrue(prompt.contains("Technology"));
        assertTrue(prompt.contains("HARD"));
        assertTrue(prompt.contains("Long (400-600 words)"));

        // Test with default/empty params
        com.linguaoptima.api.dto.request.TaskParamsRequest emptyParams = new com.linguaoptima.api.dto.request.TaskParamsRequest();
        emptyParams.setCefrLevel(CefrLevel.A2);
        emptyParams.setGrammarTopic("");
        emptyParams.setDomain("");
        emptyParams.setDifficulty(null);
        emptyParams.setMcqQuestionsCount(null);
        emptyParams.setMcqPoints(-5);
        emptyParams.setTrueFalseQuestionsCount(null);
        emptyParams.setTrueFalsePoints(0);
        emptyParams.setOpenQuestionsCount(null);
        emptyParams.setOpenPoints(0);

        String defaultPrompt = PromptTemplates.buildReadingGenerationPrompt(emptyParams, null, null);
        assertTrue(defaultPrompt.contains("Contemporary Global Issues"));
        assertTrue(defaultPrompt.contains("General"));
        assertTrue(defaultPrompt.contains("A2"));

        // Test with zero counts in direct method
        String zeroPrompt = PromptTemplates.buildReadingGenerationPrompt(
            "B1", "Travel", "Tourism", "EASY", null, null, null, null,
            0, 0, 0, 0, 0, 0
        );
        assertTrue(zeroPrompt.contains("Travel"));
        assertTrue(zeroPrompt.contains("Medium (250-400 words)"));
    }

    /**
     * @brief Tests buildSpeakingGenerationPrompt with TaskParamsRequest and length presets.
     */
    @Test
    void testBuildSpeakingGenerationPrompt() {
        TaskParamsRequest params = TaskParamsRequest.builder()
            .cefrLevel(com.linguaoptima.api.domain.enums.CefrLevel.B2)
            .grammarTopic("Pronunciation of Dental Fricatives")
            .domain("Daily Dialogue")
            .difficulty(com.linguaoptima.api.domain.enums.DifficultyLevel.MEDIUM)
            .speakingTitle("Mastering /θ/ and /ð/")
            .targetSounds("/θ/ vs /ð/")
            .speakingLength("SHORT")
            .totalPoints(100)
            .build();

        String prompt = PromptTemplates.buildSpeakingGenerationPrompt(params, null, "thought, bother, breathe");
        assertNotNull(prompt);
        assertTrue(prompt.contains("Speaking & Pronunciation reading practice"));
        assertTrue(prompt.contains("B2"));
        assertTrue(prompt.contains("/θ/ vs /ð/"));
        assertTrue(prompt.contains("Short reading passage: 30 to 50 words"));
        assertTrue(prompt.contains("thought, bother, breathe"));
        assertTrue(prompt.contains("Mastering /θ/ and /ð/"));

        // Test with LONG length
        params.setSpeakingLength("LONG");
        String longPrompt = PromptTemplates.buildSpeakingGenerationPrompt(params, null, null);
        assertTrue(longPrompt.contains("Long reading passage: 120 to 180 words"));

        // Test with CUSTOM length
        params.setSpeakingLength("CUSTOM");
        String customPrompt = PromptTemplates.buildSpeakingGenerationPrompt(params, null, null);
        assertTrue(customPrompt.contains("CUSTOM"));

        // Test default fallbacks with null fields
        TaskParamsRequest nullParams = TaskParamsRequest.builder()
            .cefrLevel(null)
            .grammarTopic(null)
            .domain(null)
            .difficulty(null)
            .speakingTitle(null)
            .targetSounds(null)
            .speakingLength(null)
            .totalPoints(null)
            .build();
        String defaultPrompt = PromptTemplates.buildSpeakingGenerationPrompt(nullParams, null, null);
        assertTrue(defaultPrompt.contains("B1"));
        assertTrue(defaultPrompt.contains("Contemporary English Communication"));
        assertTrue(defaultPrompt.contains("Natural Rhythm, Intonation & Clear Articulation"));
        assertTrue(defaultPrompt.contains("Medium reading passage: 60 to 90 words"));
    }

    @Test
    void testBuildSpeakingGenerationPromptWithPackAndEcoMode() {
        com.linguaoptima.api.dto.response.PronunciationPackResponse pack = com.linguaoptima.api.dto.response.PronunciationPackResponse.builder()
            .id("B1_DENTAL_FRICATIVES")
            .title("Dental Fricatives: /θ/ and /ð/")
            .phonemes(List.of("/θ/", "/ð/"))
            .drillWords(List.of("thought", "breathe", "weather"))
            .minimalPairs(List.of(
                com.linguaoptima.api.dto.response.MinimalPairDto.builder()
                    .word1("thin").word2("tin").phoneme1("/θ/").phoneme2("/t/").build()
            ))
            .contrastFocus("Tongue placement between teeth.")
            .tongueTwister("Thirty-three thousand feathers on a thrush's throat.")
            .build();

        TaskParamsRequest params = TaskParamsRequest.builder()
            .cefrLevel(CefrLevel.B1)
            .speakingLength("MEDIUM")
            .build();

        // With Pack and target vocabulary
        String packPrompt = PromptTemplates.buildSpeakingGenerationPrompt(params, null, "thistle, thoroughly", pack);
        assertTrue(packPrompt.contains("TARGET PHONETICS PACK: Dental Fricatives: /θ/ and /ð/"));
        assertTrue(packPrompt.contains("/θ/, /ð/"));
        assertTrue(packPrompt.contains("thought, breathe, weather"));
        assertTrue(packPrompt.contains("thin (/θ/) vs tin (/t/)"));
        assertTrue(packPrompt.contains("Tongue placement between teeth."));
        assertTrue(packPrompt.contains("Thirty-three thousand feathers on a thrush's throat."));
        assertTrue(packPrompt.contains("thistle, thoroughly"));

        // With Eco Mode
        params.setEcoMode(true);
        String ecoPrompt = PromptTemplates.buildSpeakingGenerationPrompt(params, null, null, pack);
        assertTrue(ecoPrompt.contains("ECO MODE ACTIVE"));
        assertFalse(ecoPrompt.contains("TARGET PHONETICS PACK"));

        // Direct 9-argument overload
        String directPrompt = PromptTemplates.buildSpeakingGenerationPrompt(
            "B2", "Science", "Education", "MEDIUM", "Phonetics Drill", "/r/ vs /l/", "SHORT", 100, "rocket, locker"
        );
        assertTrue(directPrompt.contains("Phonetics Drill"));
        assertTrue(directPrompt.contains("/r/ vs /l/"));
        assertTrue(directPrompt.contains("rocket, locker"));
    }

    @Test
    void testBuildSentenceExplanationPrompt() {
        // Standard usage with all fields
        String prompt = PromptTemplates.buildSentenceExplanationPrompt(
            "She would have succeeded had she tried.",
            "Historical fiction excerpt discussing Victorian ambition.",
            "B2"
        );
        assertTrue(prompt.contains("She would have succeeded had she tried."));
        assertTrue(prompt.contains("Historical fiction excerpt discussing Victorian ambition."));
        assertTrue(prompt.contains("B2"));
        assertTrue(prompt.contains("grammarBreakdown"));

        // With null / blank context and cefrLevel
        String promptDefaults = PromptTemplates.buildSentenceExplanationPrompt(
            "It rains.",
            null,
            null
        );
        assertTrue(promptDefaults.contains("It rains."));
        assertTrue(promptDefaults.contains("B1"));
        assertFalse(promptDefaults.contains("Context / Passage:"));

        // With blank context and blank cefrLevel
        String promptBlanks = PromptTemplates.buildSentenceExplanationPrompt(
            "He laughed.",
            "   ",
            "   "
        );
        assertTrue(promptBlanks.contains("He laughed."));
        assertTrue(promptBlanks.contains("B1"));

        // With null sentence
        String promptNullSentence = PromptTemplates.buildSentenceExplanationPrompt(
            null,
            "Some context",
            "C1"
        );
        assertNotNull(promptNullSentence);
        assertTrue(promptNullSentence.contains("C1"));
    }
}
