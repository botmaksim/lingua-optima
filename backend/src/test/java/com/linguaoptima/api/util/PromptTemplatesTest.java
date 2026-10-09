/**
 * @file PromptTemplatesTest.java
 * @brief Unit and slice test suite for PromptTemplates.
 */
package com.linguaoptima.api.util;

import com.linguaoptima.api.domain.enums.CefrLevel;
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
    }
}
